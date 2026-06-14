package xyz.crearts.notekeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import xyz.crearts.notekeeper.data.local.NoteDatabase
import xyz.crearts.notekeeper.data.local.SettingsDataStore
import xyz.crearts.notekeeper.data.local.TokenDataStore
import xyz.crearts.notekeeper.data.remote.RetrofitClient
import xyz.crearts.notekeeper.data.repository.NoteRepository
import xyz.crearts.notekeeper.data.sync.NetworkConnectivityObserver
import xyz.crearts.notekeeper.data.sync.SyncWorker
import xyz.crearts.notekeeper.ui.screens.LoginScreen
import xyz.crearts.notekeeper.ui.screens.NoteDetailScreen
import xyz.crearts.notekeeper.ui.screens.NoteListScreen
import xyz.crearts.notekeeper.ui.screens.SettingsScreen
import xyz.crearts.notekeeper.ui.screens.TodoDetailScreen
import xyz.crearts.notekeeper.ui.screens.TodoListScreen
import xyz.crearts.notekeeper.ui.theme.NoteKeeperTheme
import xyz.crearts.notekeeper.ui.viewmodel.LoginViewModel
import xyz.crearts.notekeeper.ui.viewmodel.NoteDetailViewModel
import xyz.crearts.notekeeper.ui.viewmodel.NoteListViewModel
import xyz.crearts.notekeeper.ui.viewmodel.TodoDetailViewModel
import xyz.crearts.notekeeper.ui.viewmodel.TodoListViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenDataStore = TokenDataStore(this)
        val settingsDataStore = SettingsDataStore(this)
        RetrofitClient.initTokenStore(tokenDataStore)

        // Load persisted server URL before any API service creation
        val savedUrl = runBlocking { settingsDataStore.serverUrl.first() }
        RetrofitClient.setBaseUrl(savedUrl)

        val database = NoteDatabase.getDatabase(this)
        val repository = NoteRepository(
            noteDao = database.noteDao(),
            apiService = RetrofitClient.noteApiService,
            attachmentApiService = RetrofitClient.attachmentApiService
        )
        val todoRepository = xyz.crearts.notekeeper.data.repository.TodoRepository(
            todoDao = database.todoDao(),
            apiService = RetrofitClient.todoApiService,
            attachmentApiService = RetrofitClient.attachmentApiService
        )

        val networkObserver = NetworkConnectivityObserver(this)
        if (savedInstanceState == null) {
            SyncWorker.syncNow(this)
        }

        setContent {
            NoteKeeperTheme {
                val navController = rememberNavController()
                NoteKeeperNavHost(
                    navController = navController,
                    repository = repository,
                    todoRepository = todoRepository,
                    networkObserver = networkObserver,
                    tokenDataStore = tokenDataStore,
                    settingsDataStore = settingsDataStore
                )
            }
        }
    }
}

@Composable
fun NoteKeeperNavHost(
    navController: NavHostController,
    repository: NoteRepository,
    todoRepository: xyz.crearts.notekeeper.data.repository.TodoRepository,
    networkObserver: NetworkConnectivityObserver,
    tokenDataStore: TokenDataStore,
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        composable("login") {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(RetrofitClient.authApiService, tokenDataStore)
            )
            LoginScreen(
                viewModel = viewModel,
                settingsDataStore = settingsDataStore,
                onLoginSuccess = {
                    navController.navigate("notes") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("notes") {
            val viewModel: NoteListViewModel = viewModel(
                factory = NoteListViewModel.Factory(repository, networkObserver)
            )
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(RetrofitClient.authApiService, tokenDataStore)
            )
            NoteListScreen(
                viewModel = viewModel,
                onNoteClick = { noteId ->
                    navController.navigate("note/$noteId")
                },
                onAddClick = {
                    navController.navigate("note/new")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                onTodosClick = {
                    navController.navigate("todos") {
                        popUpTo("notes") { inclusive = true }
                    }
                },
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("notes") { inclusive = true }
                    }
                }
            )
        }
        composable("note/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            val viewModel: NoteDetailViewModel = viewModel(
                factory = NoteDetailViewModel.Factory(repository)
            )
            NoteDetailScreen(
                viewModel = viewModel,
                noteId = if (noteId == "new") null else noteId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("todos") {
            val viewModel: TodoListViewModel = viewModel(
                factory = TodoListViewModel.Factory(todoRepository, networkObserver)
            )
            TodoListScreen(
                viewModel = viewModel,
                onTodoClick = { todoId ->
                    navController.navigate("todo/$todoId")
                },
                onAddClick = {
                    navController.navigate("todo/new")
                },
                onNotesClick = {
                    navController.navigate("notes") {
                        popUpTo("todos") { inclusive = true }
                    }
                }
            )
        }
        composable("todo/{todoId}") { backStackEntry ->
            val todoId = backStackEntry.arguments?.getString("todoId")
            val viewModel: TodoDetailViewModel = viewModel(
                factory = TodoDetailViewModel.Factory(todoRepository)
            )
            TodoDetailScreen(
                viewModel = viewModel,
                todoId = if (todoId == "new") null else todoId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

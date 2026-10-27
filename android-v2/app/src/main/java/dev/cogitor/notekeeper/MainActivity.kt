package dev.cogitor.notekeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import dev.cogitor.notekeeper.data.local.NoteDatabase
import dev.cogitor.notekeeper.data.local.SettingsDataStore
import dev.cogitor.notekeeper.data.local.TokenDataStore
import dev.cogitor.notekeeper.data.remote.RetrofitClient
import dev.cogitor.notekeeper.data.repository.NoteRepository
import dev.cogitor.notekeeper.data.sync.NetworkConnectivityObserver
import dev.cogitor.notekeeper.data.sync.SyncWorker
import dev.cogitor.notekeeper.ui.screens.LoginScreen
import dev.cogitor.notekeeper.ui.screens.NoteDetailScreen
import dev.cogitor.notekeeper.ui.screens.NoteListScreen
import dev.cogitor.notekeeper.ui.screens.SearchScreen
import dev.cogitor.notekeeper.ui.screens.SettingsScreen
import dev.cogitor.notekeeper.ui.screens.TodoDetailScreen
import dev.cogitor.notekeeper.ui.screens.TodoListScreen
import dev.cogitor.notekeeper.ui.screens.VaultDetailScreen
import dev.cogitor.notekeeper.ui.screens.VaultListScreen
import dev.cogitor.notekeeper.ui.theme.NoteKeeperTheme
import dev.cogitor.notekeeper.ui.viewmodel.LoginViewModel
import dev.cogitor.notekeeper.ui.viewmodel.NoteDetailViewModel
import dev.cogitor.notekeeper.ui.viewmodel.NoteListViewModel
import dev.cogitor.notekeeper.ui.viewmodel.TodoDetailViewModel
import dev.cogitor.notekeeper.ui.viewmodel.TodoListViewModel
import dev.cogitor.notekeeper.ui.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenDataStore = TokenDataStore(this)
        val settingsDataStore = SettingsDataStore(this)
        RetrofitClient.initTokenStore(tokenDataStore)

        val savedUrl = runBlocking { settingsDataStore.serverUrl.first() }
        RetrofitClient.setBaseUrl(savedUrl)

        val database = NoteDatabase.getDatabase(this)
        val repository = NoteRepository(
            noteDao = database.noteDao(),
            apiService = RetrofitClient.noteApiService,
            attachmentApiService = RetrofitClient.attachmentApiService
        )
        val todoRepository = dev.cogitor.notekeeper.data.repository.TodoRepository(
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
    todoRepository: dev.cogitor.notekeeper.data.repository.TodoRepository,
    networkObserver: NetworkConnectivityObserver,
    tokenDataStore: TokenDataStore,
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val route = navBackStackEntry?.destination?.route
    val showBar = route in setOf("notes", "todos", "vault", "search")

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    listOf(
                        Triple("notes", "Notes", Icons.Filled.Notes),
                        Triple("todos", "Todos", Icons.Filled.CheckCircle),
                        Triple("vault", "Vault", Icons.Filled.Lock),
                        Triple("search", "Search", Icons.Filled.Search)
                    ).forEach { (path, label, icon) ->
                        NavigationBarItem(
                            selected = route == path,
                            onClick = {
                                navController.navigate(path) {
                                    popUpTo("notes") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(padding)
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
                    onNoteClick = { navController.navigate("note/$it") },
                    onAddClick = { navController.navigate("note/new") },
                    onSettingsClick = { navController.navigate("settings") },
                    onTodosClick = { navController.navigate("todos") },
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
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable("todos") {
                val viewModel: TodoListViewModel = viewModel(
                    factory = TodoListViewModel.Factory(todoRepository, networkObserver)
                )
                TodoListScreen(
                    viewModel = viewModel,
                    onTodoClick = { navController.navigate("todo/$it") },
                    onAddClick = { navController.navigate("todo/new") },
                    onNotesClick = { navController.navigate("notes") }
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
            composable("vault") {
                val viewModel: VaultViewModel = viewModel(
                    factory = VaultViewModel.Factory(RetrofitClient.vaultApiService)
                )
                VaultListScreen(
                    viewModel = viewModel,
                    onItemClick = { navController.navigate("vault/$it") },
                    onAddClick = { navController.navigate("vault/new") }
                )
            }
            composable("vault/{itemId}") { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                val viewModel: VaultViewModel = viewModel(
                    factory = VaultViewModel.Factory(RetrofitClient.vaultApiService)
                )
                VaultDetailScreen(
                    viewModel = viewModel,
                    itemId = if (itemId == "new") null else itemId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("search") {
                SearchScreen(
                    onNoteClick = { navController.navigate("note/$it") },
                    onTodoClick = { navController.navigate("todo/$it") }
                )
            }
        }
    }
}

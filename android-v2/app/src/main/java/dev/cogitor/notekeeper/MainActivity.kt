package dev.cogitor.notekeeper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import dev.cogitor.notekeeper.ui.theme.AppThemeId
import dev.cogitor.notekeeper.ui.theme.NoteKeeperTheme
import dev.cogitor.notekeeper.ui.theme.palette
import dev.cogitor.notekeeper.ui.viewmodel.LoginViewModel
import dev.cogitor.notekeeper.ui.viewmodel.NoteDetailViewModel
import dev.cogitor.notekeeper.ui.viewmodel.NoteListViewModel
import dev.cogitor.notekeeper.ui.viewmodel.TodoDetailViewModel
import dev.cogitor.notekeeper.ui.viewmodel.TodoListViewModel
import dev.cogitor.notekeeper.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private lateinit var tokenDataStore: TokenDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tokenDataStore = TokenDataStore(this)
        val settingsDataStore = SettingsDataStore(this)
        RetrofitClient.initTokenStore(tokenDataStore)
        handleOAuthIntent(intent)

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
            val themeIdStr by settingsDataStore.themeId.collectAsState(initial = SettingsDataStore.DEFAULT_THEME)
            val themeId = AppThemeId.from(themeIdStr)
            val dark = themeId.palette.dark
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (dark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }
            NoteKeeperTheme(themeId = themeId) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "notekeeper" || data.host != "oauth") return
        val token = data.getQueryParameter("token")
        if (!token.isNullOrBlank()) {
            runBlocking { tokenDataStore.saveToken(token) }
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    listOf(
                        NavTab("notes", "Notes", Icons.AutoMirrored.Filled.Notes,
                            Icons.AutoMirrored.Outlined.Notes
                        ),
                        NavTab(
                            "todos",
                            "Todos",
                            Icons.Filled.CheckCircle,
                            Icons.Outlined.CheckCircle
                        ),
                        NavTab("vault", "Vault", Icons.Filled.Lock, Icons.Outlined.Lock),
                        NavTab("search", "Search", Icons.Filled.Search, Icons.Outlined.Search)
                    ).forEach { tab ->
                        val selected = route == tab.path
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.path) {
                                    popUpTo("notes") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

private data class NavTab(
    val path: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

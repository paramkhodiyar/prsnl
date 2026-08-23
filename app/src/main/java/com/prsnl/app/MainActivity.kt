package com.prsnl.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.prsnl.document.repository.FolderRepository
import com.prsnl.document.repository.NotebookRepository
import com.prsnl.ui.navigation.PrsnlAppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notebookRepository: NotebookRepository

    @Inject
    lateinit var folderRepository: FolderRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PrsnlAppNavHost(
                        notebookRepository = notebookRepository,
                        folderRepository = folderRepository
                    )
                }
            }
        }
    }
}

package com.example.charlesli_bookfinderapi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.charlesli_bookfinderapi.ui.theme.CharlesLiBookFinderAPITheme

// imports
import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Query

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import coil.compose.rememberImagePainter
//import kotlinx.coroutines.flow.collectAsState

//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup. moshi. kotlin. reflect. KotlinJsonAdapterFactory

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.rememberAsyncImagePainter
import retrofit2.http.Path


//tested on pixel 8a on portrait mode
//tested on pixel tablet on landscape mode

private const val BASE_URL = "https://www.googleapis.com/books/v1/"
private const val APIKEY = "oopsies-oONr5va52fMTT2SgZNYn3Xg"

class MainActivity : ComponentActivity() {
    private val googleBooksApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
            .build()
            .create(GoogleBooksAPI::class.java)
    }

    private val repository by lazy { BooksRepository(googleBooksApi) }

    private val viewModel by lazy {
        ViewModelProvider(this, BooksViewModelFactory(repository)).get(BooksViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CharlesLiBookFinderAPITheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BookApp(
                        modifier = Modifier.padding(innerPadding), viewModel = viewModel
                    )
                }
            }
        }
    }
}

interface GoogleBooksAPI{
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 10,
        @Query("key") key: String = APIKEY,
    ): BookResponse

    @GET("volumes/{id}")
    suspend fun getBookById(
        @Path("id") id: String,
        @Query("key") key: String = APIKEY,
    ): BookItem
}

data class BookResponse(
    @Json(name = "items") val books: List<BookItem>
)
data class BookItem(
    @Json(name = "id") val id: String,
    @Json(name = "volumeInfo") val volumeInfo: VolumeInfo
)
data class VolumeInfo(
    val title: String,
    val authors: List<String>?,
    @Json(name = "imageLinks") val imageLinks: ImageLinks?,
    val description: String?
)
data class ImageLinks(
    @Json(name = "thumbnail") val thumbnail: String
)

class BooksRepository(private val api: GoogleBooksAPI) {

    // Fetches books from the Google Books API
    suspend fun searchBooks(query: String, author: String, maxResults: Int = 10): List<BookItem> {
        val combinedQuery = if (author.isNotBlank()) {
            "$query+inauthor:$author"
        } else {
            query
        }
        val response = api.searchBooks(combinedQuery, maxResults)
        return response.books
    }

    suspend fun getBookById(id: String): BookItem {
        return api.getBookById(id)
    }
}
class BooksViewModel(private val repository: BooksRepository) : ViewModel() {

    private val _books = MutableStateFlow<List<BookItem>>(emptyList())
    val books: StateFlow<List<BookItem>> = _books

    private val _selectedBook = MutableStateFlow<BookItem?>(null)
    val selectedBook: StateFlow<BookItem?> = _selectedBook

    fun searchBooks(query: String, author: String) {
        viewModelScope.launch {
            val fetchedBooks = repository.searchBooks(query, author)
            _books.value = fetchedBooks
        }
    }
}

//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider

class BooksViewModelFactory(private val repository: BooksRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BooksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BooksViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
@Composable
fun BookApp(modifier: Modifier = Modifier, viewModel: BooksViewModel) {
    var selectedBook by remember { mutableStateOf<BookItem?>(null) } // Store selected book


    if (selectedBook == null) {
        BookSearch(viewModel = viewModel) { bookItem ->
            selectedBook = bookItem
        }
    } else {
        BookInfo(book = selectedBook!!) {
            selectedBook = null
        }
    }
}

@Composable
fun BookSearch(modifier: Modifier = Modifier, viewModel: BooksViewModel, onBookClick: (BookItem) -> Unit){
    var query by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }

    val books by viewModel.books.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Books!") },
            modifier = Modifier.fillMaxWidth()
                .padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = author,
            onValueChange = { author = it },
            label = { Text("Search Author") },
            modifier = Modifier.fillMaxWidth()
                .padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {viewModel.searchBooks(query, author)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            if (books.isEmpty()) {
                item {
                    Text("No books found.")
                }
            } else {
                items(books) { book ->
                    BookItem(book = book, onClick = { onBookClick(book) } )
                }
            }
        }
    }
}

@Composable
fun BookItem(book: BookItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.LightGray)
            .padding(16.dp)
            .clickable { onClick() }

    ) {

//        i give up on the image urls :) i've done everything i can think of they jsut won't work
        val thumbnailUrl = book.volumeInfo.imageLinks?.thumbnail
        val replaced = thumbnailUrl?.replace("content", "")
        Log.d("BookItem", "Thumbnail URL: $replaced")
//        if (book.volumeInfo.imageLinks?.thumbnail != null) {
        Image(
            painter = rememberAsyncImagePainter(replaced),
            contentDescription = book.volumeInfo.title,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Display the book title
        Column {
            Text(
                text = book.volumeInfo.title,
                style = MaterialTheme.typography.bodyLarge
            )

            // Display the book authors
            book.volumeInfo.authors?.let { authors ->
                Text(
                    text = "By: ${authors.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun BookInfo(book: BookItem, modifier: Modifier = Modifier, onBackClick: () -> Unit) {
    Column(modifier = modifier.padding(16.dp)) {
        // Back button
        IconButton(onClick = onBackClick) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }

        // Display the book title
        Text(
            text = book.volumeInfo.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Display the book authors
        book.volumeInfo.authors?.let { authors ->
            Text(
                text = "By: ${authors.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Display the book description
        book.volumeInfo.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Display the book thumbnail if available
        book.volumeInfo.imageLinks?.thumbnail?.let { imageUrl ->
            Image(
                painter = rememberImagePainter(imageUrl),
                contentDescription = book.volumeInfo.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        } ?: run {
            // Placeholder if no image is available
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.Gray)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CharlesLiBookFinderAPITheme {
//        BookApp()
    }
}
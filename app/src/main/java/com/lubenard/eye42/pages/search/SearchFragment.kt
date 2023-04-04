package com.lubenard.eye42.pages.search

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.lubenard.eye42.*
import com.lubenard.eye42.R
import com.lubenard.eye42.pages.details.DetailsViewModel
import com.lubenard.eye42.ui.theme.Eye42Theme
import java.util.*

class SearchFragment : Fragment() {

    private val TAG = this::class.simpleName

    private val searchViewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        MainActivity.networkManager?.connectionStatus?.observe(viewLifecycleOwner) {
            Log.d(TAG, "networkStatus is $it")
            searchViewModel.getNetworkStatus(it)
        }

        if (!getTokenFromSharedPreferences())
            MainActivity.networkManager!!.connectToApi(requireContext())

        return setContentScreen(requireContext(), findNavController(), "Welcome to Eye42 !") {
            Log.d(TAG, "Applying search page to ui")

            SearchFragmentScreen()
        }
    }

    private fun getTokenFromSharedPreferences(): Boolean {
        val sharedPreference = requireContext().getSharedPreferences("Eye42Preferences", Context.MODE_PRIVATE)

        if (sharedPreference.contains("access_token")
            && sharedPreference.contains("token_type")
            && sharedPreference.contains("token_expiration")
            && sharedPreference.contains("token_creation_date")
        ) {
            val tokenInfoRetrieved = TokenInfo(
                sharedPreference.getString("access_token", "")!!,
                sharedPreference.getString("token_type", "")!!,
                sharedPreference.getLong("token_expiration", 0),
                sharedPreference.getLong("token_creation_date", 0)
            )

            // We consider the sharedPreferences as corrupted
            if (tokenInfoRetrieved.token == "" || tokenInfoRetrieved.tokenCreatedDate == 0L || tokenInfoRetrieved.tokenExpiration == 0L) {
                Log.w(TAG, "SharedPreferences are considered corrupted")
                return false
            }

            val tokenCreationDate = Calendar.getInstance()

            tokenCreationDate.time.time = tokenInfoRetrieved.tokenCreatedDate
            tokenCreationDate.add(Calendar.SECOND, tokenInfoRetrieved.tokenCreatedDate.toInt())

            // The token is considered as expired, we need to create a new one
            if (tokenCreationDate.time.time >= Date().time) {
                Log.w(TAG, "It seems token is too old to use !")
                return false
            }

            MainActivity.networkManager?.setToken(tokenInfoRetrieved)
            return true
        }
        return false
    }


    @Composable
    fun SearchFragmentScreen() {

        val apiStatus by searchViewModel.networkStatus.collectAsState()

        SearchScreen(findNavController(), apiStatus = apiStatus, searchViewModel.connectionStatus)
    }

    @Composable
    fun SearchScreen(navController: NavController? = null, apiStatus: String, connectionStatus: ConnectionStatus) {
        var text by remember { mutableStateOf(TextFieldValue("")) }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = CenterHorizontally
        ) {
            OutlinedTextField(
                value = text,
                shape = RoundedCornerShape(50),
                maxLines = 1,
                enabled = connectionStatus == ConnectionStatus.CONNECTED,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search User button",
                        modifier = Modifier.clickable(enabled = true, onClick = {
                            if (text.text.isNotBlank() && text.text.isNotEmpty()) {
                                Log.d(TAG, "navController is $navController")
                                navController?.navigate(SearchFragmentDirections.actionSearchFragmentToDetailsFragment(text.text.lowercase()))
                            }
                        })
                    )
                },
                label = { Text(requireContext().getString(R.string.username_search_bar)) },
                onValueChange = {
                    Log.d("Searchvalue", "value is $it")
                    text = it
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (text.text.isNotBlank() && text.text.isNotEmpty()) {
                        Log.d(TAG, "navController is $navController")
                        navController?.navigate(SearchFragmentDirections.actionSearchFragmentToDetailsFragment(text.text.lowercase()))
                    }
                })
            )
            Text(apiStatus, modifier = Modifier.padding(top = 20.dp))
            if (connectionStatus == ConnectionStatus.ERROR_CONNECTING)
                Button(onClick = { MainActivity.networkManager?.connectToApi(requireContext()) }) {
                    Text(text = requireContext().getString(R.string.try_again_button))
                }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreviewSearch() {
        Eye42Theme {
            SearchScreen(apiStatus = "Trying to connect....",
                connectionStatus = ConnectionStatus.NOT_CONNECTED
            )
        }
    }
}
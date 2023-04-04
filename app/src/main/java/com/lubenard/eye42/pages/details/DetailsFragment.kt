package com.lubenard.eye42.pages.details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.lubenard.eye42.R
import com.lubenard.eye42.setContentScreen
import com.lubenard.eye42.ui.theme.Eye42Theme

class DetailsFragment : Fragment() {
    private val TAG = this::class.simpleName

    private val detailsViewModel: DetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val safeArgs: DetailsFragmentArgs by navArgs()

        detailsViewModel.loadProfileInfo(safeArgs.userName, onErrorCallback = {
            Toast.makeText(requireContext(), R.string.error_finding_user, Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return setContentScreen(context = requireContext(), findNavController(), requireContext().getString(R.string.details_page_title)) {
            DetailsFragmentScreen()
        }
    }


    @Composable
    fun DetailsFragmentScreen() {
        val profile by detailsViewModel.profile.collectAsState()
        val profileProjects by detailsViewModel.userProjects.collectAsState()
        val profileSkills by detailsViewModel.userSkills.collectAsState()
        val levelPercentage by detailsViewModel.levelPercentage.collectAsState()
        val profileLevel by detailsViewModel.userLevel.collectAsState()
        val shouldShowProjectList by detailsViewModel.shouldShowUserProjects.collectAsState()
        val shouldShowSkillsList by detailsViewModel.shouldShowUserSkills.collectAsState()

        DetailsScreen(profile, profileLevel, levelPercentage, profileProjects, profileSkills, shouldShowProjectList, shouldShowSkillsList)
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    fun DetailsScreen(
        profile: Profile?,
        profileLevel: String,
        levelPercentage: Float,
        profileProjects: List<Project>,
        skillsList: List<Pair<String, Double>>,
        shouldShowProjectList: Boolean,
        shouldShowSkillsList: Boolean
    ) {

        val scrollState = rememberScrollState()
        val projectProjectsScrollState = rememberLazyListState()
        val projectSkillsScrollState = rememberLazyListState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = scrollState, enabled = !shouldShowProjectList),
            horizontalAlignment = if (profile != null) CenterHorizontally else Alignment.End,
            verticalArrangement = if (profile == null) Arrangement.Bottom else Arrangement.Top
        ) {
            if (profile != null) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(top = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xfffaf7f7))
                ) {
                    GlideImage(
                        model = profile.imageLink,
                        modifier = Modifier
                            .padding(top = 15.dp, bottom = 15.dp)
                            .size(200.dp),
                        contentDescription = requireContext().getString(R.string.profile_image_description),
                    )
                }

                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .padding(top = 15.dp, start = 15.dp, end = 15.dp, bottom = 10.dp)
                        .weight(1f)
                        .fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xfffaf7f7))
                ) {
                    val displayName = if (profile.displayName != null) "${profile.displayName}" else "${profile.firstName} ${profile.lastName}"

                    Row(modifier = Modifier.padding(top = 15.dp).align(CenterHorizontally)) {
                        Text(
                            text = "$displayName (${profile.login})",
                            fontSize = 26.sp,
                        )
                    }

                    Box(modifier = Modifier.padding(top = 10.dp), contentAlignment = Center) {
                        LinearProgressIndicator(
                            progress = levelPercentage,
                            modifier = Modifier
                                .height(20.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp),
                            strokeCap = StrokeCap.Round
                        )
                        Text(profileLevel, color = if (levelPercentage < 0.45) MaterialTheme.colorScheme.primary else Color.White)
                    }

                    Row(modifier = Modifier
                        .padding(top = 10.dp, start = 15.dp)
                        .clickable(enabled = true) {
                            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                            i.putExtra(Intent.EXTRA_EMAIL, profile.email)
                            try {
                                requireContext().startActivity(i)
                            } catch (s: SecurityException) {
                                Toast.makeText(requireContext(), R.string.error_opening_mail_sharing, Toast.LENGTH_LONG).show()
                            }
                        }) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = requireContext().getString(R.string.email_description), modifier = Modifier.padding(end = 3.dp))
                        Text(text = profile.email)
                    }

                    Row(modifier = Modifier.padding(top = 5.dp, start = 15.dp)) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = requireContext().getString(R.string.phone_description), modifier = Modifier.padding(end = 3.dp))
                        Text(text = profile.phoneNumber, modifier = Modifier.clickable(profile.phoneNumber.isNotBlank()) {
                            if (profile.phoneNumber != "hidden") {
                                val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + profile.phoneNumber))
                                try {
                                    requireContext().startActivity(i)
                                } catch (s: SecurityException) {
                                    Toast.makeText(requireContext(), R.string.error_opening_phone_dialer, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(requireContext(), R.string.hidden_phone_number, Toast.LENGTH_SHORT).show()
                            }
                        })
                    }

                    Row(modifier = Modifier.padding(top = 5.dp, start = 15.dp)) {
                        Icon(painter = painterResource(id = R.drawable.correction_point), contentDescription = requireContext().getString(R.string.correction_point_description), modifier = Modifier.size(27.dp).padding(end = 3.dp))
                        Text(text = profile.currentCorrectionPoint.toString())
                    }

                    Row(modifier = Modifier.padding(top = 5.dp, start = 15.dp)) {
                        Icon(imageVector = Icons.Default.AccountBox, contentDescription = requireContext().getString(R.string.account_type_description), modifier = Modifier.padding(end = 3.dp))
                        Text(text = "${profile.profileType} (${if (profile.isAlumni) "alumni" else "not alumni"})")
                    }

                    if (profile.location != null) {
                        Row(modifier = Modifier.padding(top = 5.dp, start = 15.dp)) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = requireContext().getString(R.string.location_description), modifier = Modifier.padding(end = 3.dp))
                            Text(text = profile.location)
                        }
                    }

                    Row(modifier = Modifier.padding(top = 5.dp, start = 15.dp)) {
                        Icon(painter = painterResource(id = R.drawable.wallet), contentDescription = requireContext().getString(R.string.wallet_description), modifier = Modifier.size(27.dp).padding(end = 3.dp))
                        Text(text = profile.wallet.toString())
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = true) { detailsViewModel.showUserProjects() }
                    ) {
                        Text(
                            text = requireContext().getString(R.string.show_project_button),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = Bold,
                            modifier = Modifier.padding(start = 15.dp)
                        )
                        Icon(imageVector = if (shouldShowProjectList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Up arrow")
                    }
                    if (shouldShowProjectList) {
                        LazyColumn(
                            state = projectProjectsScrollState,
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.padding(start = 15.dp),
                            contentPadding = PaddingValues(top = 6.dp)
                        ) {
                            itemsIndexed(profileProjects) { _: Int, it: Project ->
                                Row(modifier = Modifier.padding(bottom = 7.dp)) {
                                    Text(it.name)
                                    if (it.note != 0) {
                                        Text("${it.note}", color = if (it.completed) Color.Green else Color.Red)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    if (it.completed) {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "Done"
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = true) { detailsViewModel.showUserSkills() }
                    ) {
                        Text(
                            text = requireContext().getString(R.string.show_skills_button),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = Bold,
                            modifier = Modifier.padding(start = 15.dp)
                        )
                        Icon(imageVector = if (shouldShowProjectList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Up arrow")
                    }
                    if (shouldShowSkillsList) {
                        LazyColumn(
                            state = projectSkillsScrollState,
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.padding(start = 15.dp),
                            contentPadding = PaddingValues(top = 6.dp)
                        ) {
                            itemsIndexed(skillsList) { _: Int, it: Pair<String, Double> ->
                                Row(modifier = Modifier.padding(bottom = 7.dp)) {
                                    Text(it.first)
                                    Spacer(Modifier.weight(1f))
                                    Text(it.second.toString().take(5))
                                }
                            }
                        }
                    }
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(size = 64.dp)
                        .padding(bottom = 5.dp, end = 5.dp),
                    color = Color.Black,
                    strokeWidth = 6.dp
                )
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun DefaultPreviewDetails() {
        Eye42Theme {
            DetailsScreen(Profile(
                "John",
                "Doe",
                "John Doe",
                "johndoe@student.42.fr",
                "jdoe",
                "hidden",
                "Student",
                "https:\\/\\/cdn.intra.42.fr\\/users\\/991654ba7cafca70e8f65aaed1bfd4c1\\/amamy.jpg",
                3,
                150,
                false,
                "Paris"
                ),
                "12.50",
                0.5f,
                listOf(),
                listOf(),
                shouldShowProjectList = false,
                shouldShowSkillsList = false
            )
        }
    }
}
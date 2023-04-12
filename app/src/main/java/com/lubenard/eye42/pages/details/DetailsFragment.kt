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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.lubenard.eye42.NetworkManager
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
        val achievementList by detailsViewModel.userAchievements.collectAsState()
        val levelPercentage by detailsViewModel.levelPercentage.collectAsState()
        val profileLevel by detailsViewModel.userLevel.collectAsState()
        val shouldShowProjectList by detailsViewModel.shouldShowUserProjects.collectAsState()
        val shouldShowSkillsList by detailsViewModel.shouldShowUserSkills.collectAsState()
        val shouldShowAchievementsList by detailsViewModel.shouldShowAchievementsList.collectAsState()

        DetailsScreen(profile, profileLevel, levelPercentage, profileProjects, profileSkills, achievementList, shouldShowProjectList, shouldShowSkillsList, shouldShowAchievementsList)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun projectDialog(projectProjectsScrollState: LazyListState, profileProjects: List<Project>) {
        Dialog(onDismissRequest = { detailsViewModel.showUserProjects() }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxHeight(0.9f)
            ) {
                LazyColumn(
                    state = projectProjectsScrollState,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(start = 15.dp),
                    contentPadding = PaddingValues(top = 6.dp)
                ) {
                    itemsIndexed(profileProjects) { _: Int, it: Project ->
                        Row(
                            modifier = Modifier.padding(bottom = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                it.name, modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .basicMarquee()
                            )
                            if (it.note != 0) {
                                Text(
                                    "${it.note}",
                                    color = if (it.completed) Color.Green else Color.Red,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            } else
                                Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (it.completed) Icons.Default.Done else Icons.Default.Close,
                                contentDescription = "Done",
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun skillsDialog(projectProjectsScrollState: LazyListState, skillsList: List<Pair<String, Double>>) {
        Dialog(onDismissRequest = { detailsViewModel.showUserSkills() }) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                LazyColumn(
                    state = projectProjectsScrollState,
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
    }

    @Composable
    fun achievementsDialog(projectProjectsScrollState: LazyListState, achievementList: List<Achievement>) {
        Dialog(onDismissRequest = { detailsViewModel.showUserAchievements() }) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                LazyColumn(
                    state = projectProjectsScrollState,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(start = 15.dp),
                    contentPadding = PaddingValues(top = 6.dp)
                ) {
                    itemsIndexed(achievementList) { _: Int, it: Achievement ->
                        Row(modifier = Modifier
                            .height(30.dp)
                            .padding(bottom = 7.dp)
                            .clickable(enabled = true) { Toast.makeText(context, it.description, Toast.LENGTH_LONG).show() }
                        ) {
                            Text("${it.name} ${"I".repeat(it.occurence)}")
                            Spacer(Modifier.weight(1f))
                            AsyncImage(
                                model = NetworkManager.apiBaseUrl.replace("/v2", "") + it.iconUrl,
                                contentDescription = "Achievement icon",
                                imageLoader = ImageLoader.Builder(LocalContext.current)
                                    .components { add(SvgDecoder.Factory()) }
                                    .build(),
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    @Preview
    fun testAchievements() {
        Column {
            achievementsDialog(
                projectProjectsScrollState = rememberLazyListState(), achievementList = listOf(
                    Achievement("Test achievement", "https://samplelib.com/lib/preview/png/sample-bumblebee-400x300.png", "", 0),
                    Achievement("Real long achievement that deserve to have it's name ellipsinging", "https://samplelib.com/lib/preview/png/sample-bumblebee-400x300.png", "", 0),
                )
            )
        }
    }

    @Composable
    @Preview()
    fun testCard() {
        projectDialog(
            rememberLazyListState(), listOf(
            Project("kfs-1", true, 100, "finished", 3),
            Project("kfs-2", false, 0, "unfinished", 3),
            Project("interniship with a very long and boring title because this is how it his", true, 100, "finished", 3),
            Project("interniship with a very long and boring title because this is how it his", false, 0, "unfinished", 3),
        ))

        skillsDialog(projectProjectsScrollState = rememberLazyListState(), skillsList = listOf(
            Pair("Unix", 14.5),
            Pair("Real long skills that deserve to have it's name ellipsinging", 150.5),
        ))
    }

    @Composable
    fun DetailsScreen(
        profile: Profile?,
        profileLevel: String,
        levelPercentage: Float,
        profileProjects: List<Project>,
        skillsList: List<Pair<String, Double>>,
        achievementList: List<Achievement>,
        shouldShowProjectList: Boolean,
        shouldShowSkillsList: Boolean,
        shouldShowAchievementsList: Boolean
    ) {

        val scrollState = rememberScrollState()
        val projectProjectsScrollState = rememberLazyListState()
        val projectSkillsScrollState = rememberLazyListState()

        val context = LocalContext.current

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
                    AsyncImage(
                        model = profile.imageLink,
                        contentDescription = requireContext().getString(R.string.profile_image_description),
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .size(150.dp)
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

                    Row(modifier = Modifier
                        .padding(top = 15.dp)
                        .align(CenterHorizontally)) {
                        Text(text = "$displayName (${profile.login})", fontSize = 26.sp,)
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

                    TextIconComponent(
                        Icons.Default.Email,
                        iconDescription = context.getString(R.string.email_description),
                        text = profile.email,
                        onClick = {
                            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                            i.putExtra(Intent.EXTRA_EMAIL, profile.email)
                            try {
                                requireContext().startActivity(i)
                            } catch (s: SecurityException) {
                                Toast
                                    .makeText(
                                        requireContext(),
                                        R.string.error_opening_mail_sharing,
                                        Toast.LENGTH_LONG
                                    )
                                    .show()
                            }
                        }
                    )

                    if (profile.phoneNumber.isNotBlank()) {
                        TextIconComponent(
                            Icons.Default.Phone,
                            iconDescription = context.getString(R.string.phone_description),
                            text = profile.phoneNumber,
                            onClick = {
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
                            }
                        )
                    }

                    TextIconComponent(
                        icon = R.drawable.correction_point,
                        iconDescription = context.getString(R.string.correction_point_description),
                        text = profile.currentCorrectionPoint.toString()
                    )

                    TextIconComponent(
                        Icons.Default.AccountBox,
                        iconDescription = context.getString(R.string.account_type_description),
                        text = "${profile.profileType} (${if (profile.isAlumni) "alumni" else "not alumni"})"
                    )

                    if (profile.location != null) {
                        TextIconComponent(
                            Icons.Default.LocationOn,
                            iconDescription = context.getString(R.string.location_description),
                            text = profile.location
                        )
                    }

                    TextIconComponent(
                        icon = R.drawable.wallet,
                        iconDescription = context.getString(R.string.wallet_description),
                        text = profile.wallet.toString()
                    )

                    Text(
                        text = requireContext().getString(R.string.show_project_button),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 15.dp)
                            .clickable(enabled = true) { detailsViewModel.showUserProjects() }
                    )
                    if (shouldShowProjectList) {
                        projectDialog(projectProjectsScrollState, profileProjects = profileProjects)
                    }

                    Text(
                        text = requireContext().getString(R.string.show_skills_button),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 15.dp)
                            .clickable(enabled = true) { detailsViewModel.showUserSkills() }
                    )
                    if (shouldShowSkillsList) {
                        skillsDialog(projectProjectsScrollState = projectSkillsScrollState, skillsList = skillsList)
                    }

                    Text(
                        text = requireContext().getString(R.string.show_achievements_button),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 15.dp)
                            .clickable(enabled = true) { detailsViewModel.showUserAchievements() }
                    )
                    if (shouldShowAchievementsList) {
                        achievementsDialog(projectProjectsScrollState = projectSkillsScrollState, achievementList = achievementList)
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

    @Composable
    fun TextIconComponent(iconVector: ImageVector?= null, icon: Int? = null, iconDescription: String, text: String, onClick: (() -> Unit)? = null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp, start = 15.dp)
                .clickable(enabled = onClick != null) {
                    onClick?.invoke()
                }
        ) {
            if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = iconDescription,
                    modifier = Modifier.padding(end = 3.dp)
                )
            } else if (icon != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = iconDescription,
                    modifier = Modifier
                        .size(27.dp)
                        .padding(end = 3.dp)
                )
            }
            Text(text = text)
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun TextIconComponentPreview() {
        Column() {
            TextIconComponent(
                Icons.Default.Email,
                iconDescription = "Wallet", text = "3"
            )
            TextIconComponent(
                Icons.Default.AccountBox,
                iconDescription = "Account details", text = "Paris"
            )
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
                listOf(),
                shouldShowProjectList = false,
                shouldShowSkillsList = false,
                shouldShowAchievementsList = false
            )
        }
    }
}
package com.lubenard.eye42.pages.details

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.Request
import com.lubenard.eye42.MainActivity
import com.lubenard.eye42.checkJsonFields
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

data class Profile(
    val firstName: String,
    val lastName: String,
    val displayName: String? = null,
    val email: String,
    val login: String,
    val phoneNumber: String,
    val profileType: String,
    val imageLink: String,
    val currentCorrectionPoint: Int,
    val wallet: Int,
    val isAlumni: Boolean,
    val location: String?,
    val locationLink: String?,
    val currentTitle: String
)

data class Project(
    val name: String,
    val completed: Boolean,
    val note: Int,
    val status: String,
    val retryNumber: Int
)

data class Achievement(
    val name: String,
    val iconUrl: String,
    val description: String,
    val occurence: Int = 1
)

class DetailsViewModel : ViewModel() {

    private val TAG = this::class.simpleName

    val profile = MutableStateFlow<Profile?>(null)
    val userProjects = MutableStateFlow<List<Project>>(listOf())
    val userSkills = MutableStateFlow<List<Pair<String, Double>>>(listOf())
    val userAchievements = MutableStateFlow<List<Achievement>>(listOf())

    val levelPercentage = MutableStateFlow(0f)
    val userLevel = MutableStateFlow("")

    val shouldShowUserProjects = MutableStateFlow(false)
    val shouldShowUserSkills = MutableStateFlow(false)
    val shouldShowAchievementsList = MutableStateFlow(false)

    fun loadProfileInfo(userName: String, onErrorCallback: (() -> Unit)) {
        Log.d(TAG, "Looking for username $userName")
        MainActivity.networkManager?.getServerResponse(Request.Method.GET, "/users/$userName", successCallback = {
            Log.d("DetailsViewModel", "Success about getting info about me !")
            Log.d("DetailsViewModel", "Response is $it")

            if (checkJsonFields(it, listOf("first_name", "last_name", "email", "login", "phone", "image", "correction_point", "wallet", "location"))) {
                profile.value = Profile(
                    it.getString("first_name"),
                    it.getString("last_name"),
                    if (it.has("displayname")) it.getString("displayname") else null,
                    it.getString("email"),
                    it.getString("login"),
                    it.getString("phone"),
                    if (it.has("kind")) it.getString("kind") else "Unknown",
                    it.getJSONObject("image").getString("link"),
                    it.getInt("correction_point"),
                    it.getInt("wallet"),
                    if (it.has("alumni?")) it.getBoolean("alumni?") else true,
                    if (it.isNull("location")) null else it.getString("location"),
                    "", // We cannot get the campus link without exploring "campus"
                    "" // We cannot get the title link without exploring "titles"
                )
                Log.d("Details", "Profile is ${profile.value}")
            }

            // If we cannot get campus based on first json Object, we try to find it via campus field
            if (it.has("campus") && profile.value?.location == null) {
                val campus = it.getJSONArray("campus")[it.getJSONArray("campus").length() - 1] as JSONObject
                profile.value = profile.value?.copy(location = campus.getString("name"), locationLink = campus.getString("website"))
            }

            if (it.has("projects_users")) {
                Log.d(TAG, "Users has project !")
                val projectList: MutableList<Project> = mutableListOf()
                val jsonUserProjects = it.getJSONArray("projects_users")
                for (i in 0 until  jsonUserProjects.length()) {
                    val currentProject = (jsonUserProjects[i] as JSONObject)
                    projectList.add(Project(
                        name = (currentProject["project"] as JSONObject).getString("name"),
                        completed = if (currentProject.has("validated?") && !currentProject.isNull("validated?")) currentProject.getBoolean("validated?") else false,
                        note = if (currentProject.has("validated?") && !currentProject.isNull("validated?")) currentProject.getInt("final_mark") else 0,
                        status = currentProject.getString("status"),
                        retryNumber = currentProject.getInt("occurrence")
                    ))
                    userProjects.value = projectList.toList()
                }
            }

            if (it.has("cursus_users")) {
                val skillsList: MutableList<Pair<String, Double>> = mutableListOf()
                val jsonLatestCursus = (it["cursus_users"] as JSONArray)[(it["cursus_users"] as JSONArray).length() - 1] as JSONObject
                val jsonUserSkills = jsonLatestCursus.getJSONArray("skills")
                if (jsonLatestCursus.has("level")) {
                    userLevel.value =  jsonLatestCursus.getDouble("level").toString().take(5)
                    levelPercentage.value = jsonLatestCursus.getDouble("level").toFloat() % 1
                }
                for (i in 0 until  jsonUserSkills.length()) {
                    val currentProject = (jsonUserSkills[i] as JSONObject)
                    skillsList.add(Pair(currentProject.getString("name"), currentProject.getDouble("level")))
                    userSkills.value = skillsList.toList()
                }
            }

            if (it.has("achievements")) {
                val achievementList = it.getJSONArray("achievements")
                val achievementListMutable: MutableList<Achievement> = mutableListOf()
                for (i in 0 until achievementList.length()) {
                    val achievement = (achievementList[i] as JSONObject)
                    val achievementName = achievement.getString("name")
                    val searchOccurence = achievementListMutable.find { it.name == achievementName }
                    if (searchOccurence != null) {
                        achievementListMutable.remove(searchOccurence)
                    }
                    achievementListMutable.add(
                        Achievement(
                            achievement.getString("name"),
                            achievement.getString("image"),
                            achievement.getString("description"),
                            if (searchOccurence != null) searchOccurence.occurence + 1 else 0
                        )
                    )
                }
                userAchievements.value = achievementListMutable
            }

            if (it.has("titles") && it.has("titles_users")) {
                val titlesJson = it.getJSONArray("titles")
                val userTitles = mutableListOf<Pair<Int, String>>()
                for (j in  0 until titlesJson.length()) {
                    userTitles.add(Pair((titlesJson[j] as JSONObject).getInt("id"), (titlesJson[j] as JSONObject).getString("name")))
                }
                Log.d(TAG, "userTitles are $userTitles")
                val titlesUsersJson = it.getJSONArray("titles_users")
                for (i in 0 until titlesUsersJson.length()) {
                    if ((titlesUsersJson[i] as JSONObject).getBoolean("selected")) {
                        Log.d(TAG, "${(titlesUsersJson[i] as JSONObject)}")
                        val currentTitle = userTitles.find { it.first == (titlesUsersJson[i] as JSONObject).getInt("title_id") }
                        Log.d(TAG, "currentTitle is $currentTitle")
                        if (currentTitle != null)
                            profile.value = profile.value?.copy(currentTitle = currentTitle.second)
                    }
                }
            }
        },
        errorCallback = {
            onErrorCallback.invoke()
        })
    }

    fun showUserProjects() {
        Log.d(TAG, "${if (!shouldShowUserProjects.value) "SHOW" else "HIDE"} dialog")
        shouldShowUserProjects.value = !shouldShowUserProjects.value
    }

    fun showUserSkills() {
        shouldShowUserSkills.value = !shouldShowUserSkills.value
    }

    fun showUserAchievements() {
        shouldShowAchievementsList.value = !shouldShowAchievementsList.value
    }
}
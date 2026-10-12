package com.example.academyapp.course.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class LearningRouteSerializationTest {
    @Test
    fun polymorphicKeysRoundTripWithoutReflection() {
        val json = Json { serializersModule = learningSerializers }
        val serializer = ListSerializer(PolymorphicSerializer(NavKey::class))
        val routes = listOf<NavKey>(LearningRoute.Dashboard, LearningRoute.Curriculum(42), LearningRoute.LessonDetail(42, 7), LearningRoute.QuizWeb(42, 9), LearningRoute.Courses)
        assertEquals(routes, json.decodeFromString(serializer, json.encodeToString(serializer, routes)))
    }
}

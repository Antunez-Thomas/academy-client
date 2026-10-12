package com.example.academyapp.api

internal object TestFixtures {
    const val AUTH_OPTIONS = """{"registrationAvailable":true,"legalAgreementRequired":true,"termsUrl":"https://example.test/terms","privacyUrl":"https://example.test/privacy"}"""
    const val PASSWORD_RESET_RESPONSE = """{"ok":true,"message":"If an account exists for this email, password-reset instructions will be sent."}"""

    const val LOGIN_RESPONSE = """
        {
          "user":{"id":42,"email":"learner@example.test","displayName":"Test Learner"},
          "accessToken":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
          "refreshToken":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
          "accessTokenExpiresAt":"2026-09-08T18:30:00+00:00"
        }
    """

    const val USER = """
        {"id":42,"email":"learner@example.test","displayName":"Test Learner"}
    """

    const val COURSES = """
        [{
          "id":1001,
          "title":"Example course",
          "excerpt":"A stable fixture for client contract tests.",
          "imageUrl":"https://example.test/course.jpg",
          "progressPercent":25,
          "categories":["Professional development"]
        }]
    """

    const val DASHBOARD = """
        {
          "courses": [{"id":1001,"title":"Example course","excerpt":"A stable fixture.","imageUrl":null,"progressPercent":25,"categories":[]}],
          "upcomingLiveClasses":{"status":"ready","items":[{"courseId":1001,"lessonId":2002,"courseTitle":"Example course","title":"Clase de prueba","startAt":"2026-10-07T18:00:00+00:00","endAt":"2026-10-07T19:00:00+00:00","joinable":true,"joinUrl":"https://us02web.zoom.us/j/123456"}]},
          "events":{"status":"ready","items":[{"title":"Evento &amp; especial","startAt":"2026-10-07T18:00:00Z","endAt":"2026-10-07T19:00:00Z","status":"starting","joinable":true,"actionLabel":"Ingresar al evento","joinUrl":"https://events.example.test/join/123"}]},
          "supportActions":{"status":"ready","items":[{"label":"WhatsApp","kind":"whatsapp","url":"https://wa.me/5491112345678"}]}
        }
    """

    const val COURSE = """
        {
          "id":1001,
          "title":"Example course",
          "excerpt":"A stable fixture for client contract tests.",
          "imageUrl":"https://example.test/course.jpg",
          "progressPercent":25,
          "categories":["Professional development"]
        }
    """

    const val LESSON = """
        {
          "id":2002,
          "courseId":1001,
          "parentLessonId":2001,
          "type":"topic",
          "title":"First video",
          "position":0,
          "durationSeconds":null,
          "completed":false,
          "positionSeconds":12,
          "video":{"videoId":"018f47ac-38d7-7e45-a5d9-82018f4b87fb","libraryId":12345},
          "resources":[
            {"id":7101,"title":"Course reading","mediaType":"application/pdf","url":"https://example.test/wp-content/uploads/2026/01/reading.pdf"},
            {"id":7102,"title":"Presentation","mediaType":"application/vnd.openxmlformats-officedocument.presentationml.presentation","url":"https://example.test/wp-content/uploads/2026/01/presentation.pptx"},
            {"id":7103,"title":"Legacy presentation","mediaType":"application/vnd.ms-powerpoint","url":"https://example.test/wp-content/uploads/2026/01/legacy.ppt"}
          ]
        }
    """

    const val COURSE_CONTENT = """
        {
          "course":$COURSE,
          "steps":[$LESSON],
          "quizzes":[{
            "id":3001,
            "courseId":1001,
            "title":"Knowledge check",
            "position":1,
            "completed":false
          }]
        }
    """

    const val QUIZ_WEB_LAUNCH = """
        {
          "userId":42,
          "courseId":1001,
          "quizId":3001,
          "ticket":"dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
          "redeemPath":"/wp-json/academy-mobile/v1/quiz/web-redeem",
          "logoutPath":"/wp-json/academy-mobile/v1/quiz/web-logout",
          "quizPath":"/quizzes/knowledge-check/?lesson=1001",
          "expiresAt":"2026-10-02T13:05:00+00:00"
        }
    """

    const val PROGRESS_RESPONSE = """
        {
          "lessonId":2002,
          "courseId":1001,
          "completed":false,
          "positionSeconds":120,
          "accepted":true,
          "clientEventId":"018f47ac-38d7-7e45-a5d9-82018f4b87fb",
          "duplicate":false
        }
    """

    const val PLAYBACK_RESPONSE = """
        {
          "lessonId":2002,
          "videoId":"018f47ac-38d7-7e45-a5d9-82018f4b87fb",
          "libraryId":12345,
          "token":"cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
          "expires":1788892500,
          "tokenScheme":"embed_sha256_hex",
          "playbackUrl":"https://iframe.mediadelivery.net/embed/12345/018f47ac-38d7-7e45-a5d9-82018f4b87fb?token=redacted&expires=1788892500",
          "expiresAt":"2026-09-08T18:35:00+00:00"
        }
    """

    const val ENTITLEMENTS = """
        [{
          "userId":42,
          "courseId":1001,
          "source":"website",
          "status":"active",
          "expiresAt":null,
          "transactionId":null
        }]
    """

    const val COURSE_PROGRESS = """{"courseId":1001,"progressPercent":25}"""
    const val LOGOUT = """{"ok":true}"""
    const val API_ERROR = """
        {"code":"invalid_credentials","message":"Invalid credentials.","data":{"status":401}}
    """
}

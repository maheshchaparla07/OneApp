package uk.ac.tees.mad.s3445191

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import java.util.Date


data class WebApp(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val userId: String = "",
    val createdAt: Date? = null,
    val iconUrl: String? = null,

)
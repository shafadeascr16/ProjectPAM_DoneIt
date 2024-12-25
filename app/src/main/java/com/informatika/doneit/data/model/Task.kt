package com.informatika.doneit.data.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Task(
    @DocumentId var id: String = UUID.randomUUID().toString(),
    var userId: String = "",
    var title: String = "",
    var description: String = "",
    var priority: String = "",
    var dueDate: String = "",
    var location: String = "",
    var completed: Boolean = false
) : Parcelable

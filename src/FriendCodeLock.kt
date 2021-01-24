package co.simonkenny.web

class FriendCodeLock {

    var friendCode: String? = null

    fun unlock(code: String): Boolean = friendCode?.let { it == code } == true

    companion object {
        private val INSTANCE = FriendCodeLock()

        fun getInstance() = INSTANCE
    }
}
package graphene.rpc

data class Node(
    val name: String,
    val url: String,
    val username: String = "",
    val password: String = ""
)

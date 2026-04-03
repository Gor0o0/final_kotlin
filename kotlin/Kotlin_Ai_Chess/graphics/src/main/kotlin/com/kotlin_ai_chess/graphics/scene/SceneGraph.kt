package com.kotlin_ai_chess.graphics.scene

import com.kotlin_ai_chess.shared.math.Vector3

class Scene {
    val nodes = mutableListOf<Node>()
}

data class Node(
    val name: String,
    val children: MutableList<Node> = mutableListOf(),
    val transform: Transform = Transform()
)

data class Transform(
    var position: Vector3 = Vector3(0f, 0f, 0f),
    var rotation: Vector3 = Vector3(0f, 0f, 0f),
    var scale: Vector3 = Vector3(1f, 1f, 1f)
)

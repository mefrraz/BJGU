package com.bjgu.app.challenges

/**
 * Representa um desafio matemático gerado pelo [ChallengeGenerator].
 *
 * @property question A pergunta em formato texto (ex: "23 × 7 = ?").
 * @property answer A resposta numérica esperada (ex: 161).
 */
data class MathChallenge(
    val question: String,
    val answer: Int
)

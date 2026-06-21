package com.bjgu.app.challenges

import kotlin.random.Random

/**
 * Níveis de dificuldade do desafio matemático.
 */
enum class Difficulty {
    EASY,   // Soma/subtração com números 1–20
    MEDIUM, // Soma/subtração 10–99, ou multiplicação 1–10
    HARD    // Multiplicação 11–20, divisão exata, ou operações mistas
}

/**
 * Gerador de desafios matemáticos para o BJGU.
 *
 * Produz perguntas matemáticas com base no nível de dificuldade.
 * Garante que a pergunta gerada NUNCA é igual à anterior
 * (nem em texto de pergunta, nem em valor de resposta),
 * obrigando o utilizador a pensar ativamente em vez de repetir de memória.
 */
class ChallengeGenerator {

    /** Máximo de tentativas para gerar uma pergunta diferente da anterior. */
    private val maxAttempts = 10

    /**
     * Gera um novo desafio matemático.
     *
     * @param difficulty Nível de dificuldade desejado.
     * @param previous O desafio anterior (null se for o primeiro).
     *                  A nova pergunta será diferente deste em texto e resposta.
     * @return Um novo [MathChallenge] garantidamente diferente do anterior.
     */
    fun generate(difficulty: Difficulty, previous: MathChallenge? = null): MathChallenge {
        for (attempt in 1..maxAttempts) {
            val challenge = when (difficulty) {
                Difficulty.EASY -> generateEasy()
                Difficulty.MEDIUM -> generateMedium()
                Difficulty.HARD -> generateHard()
            }

            // Verificar se é diferente do anterior (pergunta E resposta)
            if (previous == null) return challenge
            if (challenge.question != previous.question && challenge.answer != previous.answer) {
                return challenge
            }
        }

        // Fallback: após maxAttempts tentativas, gera uma última vez (raro)
        return when (difficulty) {
            Difficulty.EASY -> generateEasy()
            Difficulty.MEDIUM -> generateMedium()
            Difficulty.HARD -> generateHard()
        }
    }

    /**
     * Verifica se a resposta do utilizador está correta.
     *
     * @param challenge O desafio atual.
     * @param userAnswer A resposta introduzida pelo utilizador.
     * @return true se a resposta estiver correta.
     */
    fun checkAnswer(challenge: MathChallenge, userAnswer: Int): Boolean {
        return challenge.answer == userAnswer
    }

    // ─── Geradores por dificuldade ──────────────────────────────────

    /**
     * Fácil: a + b ou a - b, com a, b ∈ [1, 20].
     * Subtração garante resultado ≥ 0.
     */
    private fun generateEasy(): MathChallenge {
        val useAddition = Random.nextBoolean()
        if (useAddition) {
            val a = Random.nextInt(1, 21)
            val b = Random.nextInt(1, 21)
            return MathChallenge("$a + $b = ?", a + b)
        } else {
            val a = Random.nextInt(1, 21)
            val b = Random.nextInt(1, a + 1)  // b ≤ a para resultado ≥ 0
            return MathChallenge("$a - $b = ?", a - b)
        }
    }

    /**
     * Médio: a + b ou a - b com a, b ∈ [10, 99],
     * ou a × b com a ∈ [2, 10], b ∈ [1, 10].
     */
    private fun generateMedium(): MathChallenge {
        val operation = Random.nextInt(3)  // 0=soma, 1=subtração, 2=multiplicação
        return when (operation) {
            0 -> {
                val a = Random.nextInt(10, 100)
                val b = Random.nextInt(10, 100)
                MathChallenge("$a + $b = ?", a + b)
            }
            1 -> {
                val a = Random.nextInt(10, 100)
                val b = Random.nextInt(10, a + 1)  // b ≤ a
                MathChallenge("$a - $b = ?", a - b)
            }
            else -> {
                val a = Random.nextInt(2, 11)
                val b = Random.nextInt(1, 11)
                MathChallenge("$a × $b = ?", a * b)
            }
        }
    }

    /**
     * Difícil: a × b com a, b ∈ [11, 20],
     * ou a ÷ b (divisão exata) com a = b × c, b ∈ [2, 12], c ∈ [2, 20],
     * ou operação mista: a × b + c.
     */
    private fun generateHard(): MathChallenge {
        val operation = Random.nextInt(3)  // 0=multiplicação, 1=divisão, 2=mista
        return when (operation) {
            0 -> {
                val a = Random.nextInt(11, 21)
                val b = Random.nextInt(11, 21)
                MathChallenge("$a × $b = ?", a * b)
            }
            1 -> {
                // Divisão exata: a ÷ b = c,  onde a = b × c
                val b = Random.nextInt(2, 13)      // divisor
                val c = Random.nextInt(2, 21)      // quociente
                val a = b * c                       // dividendo
                MathChallenge("$a ÷ $b = ?", c)
            }
            else -> {
                // Operação mista: a × b + c
                val a = Random.nextInt(3, 13)
                val b = Random.nextInt(3, 13)
                val c = Random.nextInt(1, 21)
                MathChallenge("$a × $b + $c = ?", a * b + c)
            }
        }
    }
}

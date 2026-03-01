package ai.agent.vibecoding

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.Scanner

@SpringBootApplication
class VibeCodingApplication(
    private val agentService: VibeCodingAgentService
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val scanner = Scanner(System.`in`)
        val currentWorkingDirectory = System.getProperty("user.dir")

        println("==================================================")
        println("🚀 [AGENT] coding-ai-agent CLI 모드 가동 완료")
        println("📂 현재 작업 디렉토리: $currentWorkingDirectory")
        println("💡 명령어 안내:")
        println("   - '/plan <내용>' : 수석 아키텍트(Gemini) 설계 후 실무자(Claude) 구현")
        println("   - '<일반 텍스트>' : 실무자(Claude)에게 바로 코드 수정 및 질문 지시")
        println("   - 'clear' 또는 'reset' : 대화 맥락 및 설계도 초기화")
        println("   - 'exit' 또는 'quit'   : 에이전트 종료")
        println("==================================================")

        while (true) {
            print("agent> ")
            val input = scanner.nextLine().trim()

            if (input.isEmpty()) continue

            when {
                input.lowercase() in listOf("exit", "quit") -> {
                    println("👋 [AGENT] 작업을 종료합니다!")
                    break
                }
                input.lowercase() in listOf("clear", "reset") -> {
                    agentService.clearMemory()
                    println("🧹 [AGENT] 이전 작업 맥락과 설계도가 초기화되었습니다.")
                }
                input.startsWith("/plan ") -> {
                    val prompt = input.removePrefix("/plan ").trim()
                    agentService.executeWithArchitect(prompt, currentWorkingDirectory)
                }
                else -> {
                    // 일반 텍스트 입력 시 Claude 실무자만 호출
                    agentService.executeDeveloperOnly(input, currentWorkingDirectory)
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<VibeCodingApplication>(*args)
}
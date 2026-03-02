package ai.agent.vibecoding

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Description
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.stream.Collectors

@Configuration
class AgentTools {
    data class FileReadRequest(val absolutePath: String)
    data class FileWriteRequest(val absolutePath: String, val content: String)
    data class CommandRequest(val command: String, val workingDirectory: String)

    @Bean
    @Description("주어진 절대 경로의 파일 내용을 읽어옵니다.")
    fun readFile(): Function<FileReadRequest, String> = Function { request ->
        try {
            val path = Paths.get(request.absolutePath)
            if (Files.exists(path)) Files.readString(path) else "Error: 파일 없음 - ${request.absolutePath}"
        } catch (e: Exception) { "Error: ${e.message}" }
    }

    @Bean
    @Description("주어진 절대 경로에 코드를 덮어씁니다. 상위 폴더가 없으면 생성합니다.")
    fun writeCodeToFile(): Function<FileWriteRequest, String> = Function { request ->
        try {
            val path = Paths.get(request.absolutePath)
            path.parent?.let { if (!Files.exists(it)) Files.createDirectories(it) }
            Files.writeString(path, request.content)
            "Success: 저장 완료 -> ${request.absolutePath}"
        } catch (e: Exception) { "Error: ${e.message}" }
    }

    @Bean
    @Description("터미널 명령어(예: ./gradlew test)를 실행하고 콘솔 출력을 반환합니다. 최대 30초의 실행 시간 제한이 있습니다.")
    fun executeShellCommand(): Function<CommandRequest, String> = Function { request ->
        try {
            val builder = ProcessBuilder("bash", "-c", request.command)
            builder.directory(Paths.get(request.workingDirectory).toFile())
            builder.redirectErrorStream(true)

            val process = builder.start()

            // 💡 최대 30초까지만 대기합니다.
            val isFinished = process.waitFor(300, TimeUnit.SECONDS)

            if (!isFinished) {
                // 타임아웃 발생 시 프로세스를 강제 종료하고 에이전트에게 경고 메시지를 반환합니다.
                process.destroyForcibly()
                return@Function "Execution Failed: ⏰ 타임아웃 발생 (300초 초과). 코드가 무한 루프에 빠졌거나 사용자 입력을 기다리고 있을 수 있습니다. 코드를 검토하고 다시 수정하세요."
            }

            val output = BufferedReader(InputStreamReader(process.inputStream)).lines().collect(Collectors.joining("\n"))
            "Command Output:\n$output"
        } catch (e: Exception) {
            "Execution Failed: ${e.message}"
        }
    }
}
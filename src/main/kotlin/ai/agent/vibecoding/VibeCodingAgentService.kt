package ai.agent.vibecoding

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.InMemoryChatMemory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Service
class VibeCodingAgentService(
    @Qualifier("openAiChatModel") private val geminiModel: ChatModel,
    @Qualifier("anthropicChatModel") private val claudeModel: ChatModel,
    private val loggingAdvisor: AgentActionLoggingAdvisor
) {
    private val geminiArchitect = ChatClient.builder(geminiModel).build()
    private val claudeDeveloper = ChatClient.builder(claudeModel).build()
    private val chatMemory = InMemoryChatMemory()
    private var currentConversationId = UUID.randomUUID().toString()

    // 💡 수석 아키텍트의 최신 설계 문서를 기억할 변수
    private var lastArchitecturePlan: String = "아직 수석 아키텍트의 설계 문서가 없습니다. 사용자 지시사항에 따라 바로 코딩을 진행하세요."

    fun clearMemory() {
        currentConversationId = UUID.randomUUID().toString()
        lastArchitecturePlan = "아직 수석 아키텍트의 설계 문서가 없습니다." // 설계도 함께 초기화
    }

    // 🌟 모드 1: 새로운 기능 기획 (Gemini + Claude)
    fun executeWithArchitect(userPrompt: String, basePath: String) {
        val projectContext = readProjectRules(basePath)

        println("\n🚀 [Phase 1: Gemini Architect] 요구사항 분석 및 설계 중...")
        val architectPrompt = """
            당신은 15년 차 수석 아키텍트입니다. 요구사항을 분석하여 타겟 프로젝트에 맞는 최적의 구현 방법을 마크다운으로 상세히 작성하세요.
        """.trimIndent()

        val architecturePlan = geminiArchitect.prompt()
            .system(architectPrompt)
            .user(userPrompt)
            .call()
            .content() ?: "설계 문서 생성 실패"

        this.lastArchitecturePlan = architecturePlan
        println("✅ [Gemini 설계 완료]\n")

        // 이어서 Claude 호출
        callClaudeDeveloper(userPrompt, basePath, projectContext)
    }

    // 🌟 모드 2: 단순 수정 및 대화 (Claude Only)
    fun executeDeveloperOnly(userPrompt: String, basePath: String) {
        val projectContext = readProjectRules(basePath)
        println("\n🚀 [Phase 2: Claude Developer] 실무 개발자가 코드 수정 및 환경 제어를 시작합니다...")
        callClaudeDeveloper(userPrompt, basePath, projectContext)
    }

    // 🛠️ Claude 호출 공통 로직
    private fun callClaudeDeveloper(userPrompt: String, basePath: String, projectContext: String) {
        val systemPrompt = """
            당신은 터미널에서 사용자와 대화하며 코드를 작성하는 최고 수준의 AI 에이전트입니다.
            [작업 경로]: $basePath
            [프로젝트 규칙]: $projectContext
            
            **[핵심 임무]**
            - 수석 아키텍트의 설계(있다면)를 바탕으로 'readFile', 'writeCodeToFile' 도구를 사용해 코드를 구현하세요.
            - 수정 후에는 'executeShellCommand'로 테스트를 돌려보고 결과를 파악하세요.
            - 이전 대화 맥락을 기억하고 있으므로 자연스럽게 대응하세요.
            
            **⚠️ [안전 및 무한 루프 방지 수칙]**
            1. (최대 시도 제한): 코드 수정 및 테스트 실행의 반복은 하나의 목표당 **최대 5번까지만 시도**하세요.
            2. (강제 종료): 5번의 시도 후에도 에러가 해결되지 않으면 즉시 도구 사용을 멈추고 사용자에게 보고하세요.
            3. (타임아웃 인지): 쉘 커맨드 실행 결과가 '타임아웃 발생'으로 오면 무한 루프를 의심하고 코드를 수정하세요.
        """.trimIndent()

        val currentInput = "사용자 요구사항: $userPrompt\n\n[참고용] 최근 아키텍트 설계 문서:\n${this.lastArchitecturePlan}"

        val response = claudeDeveloper.prompt()
            .system(systemPrompt)
            .user(currentInput)
            .advisors(
                MessageChatMemoryAdvisor(chatMemory, currentConversationId, 100),
                loggingAdvisor
            )
            .functions("readFile", "writeCodeToFile", "executeShellCommand")
            .call()
            .content() ?: "응답을 생성하지 못했습니다."

        println("\n🤖 [Agent]: $response\n")
    }

    private fun readProjectRules(basePath: String): String {
        return try {
            val path = Paths.get(basePath, "CLAUDE.md")
            if (Files.exists(path)) Files.readString(path) else "특별한 프로젝트 규칙 없음."
        } catch (e: Exception) { "규칙 파일 읽기 실패" }
    }
}
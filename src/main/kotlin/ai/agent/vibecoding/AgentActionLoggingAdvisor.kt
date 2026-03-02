package ai.agent.vibecoding

import org.springframework.ai.chat.client.advisor.api.*
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.stereotype.Component

@Component
class AgentActionLoggingAdvisor : CallAroundAdvisor {
    override fun getName() = "AgentActionLoggingAdvisor"
    override fun getOrder() = 0

    override fun aroundCall(advisedRequest: AdvisedRequest, chain: CallAroundAdvisorChain): AdvisedResponse {
        val response = chain.nextAroundCall(advisedRequest)
        response.response?.results?.forEach { result ->
            val msg = result.output
            if (msg is AssistantMessage) {
                if (msg.hasToolCalls()) {
                    println("\n🧠 [Agent Tool Use]")
                    msg.toolCalls?.forEach { tool -> println("   🛠️ ${tool.name} -> ${tool.arguments}") }
                } else {
                    println("\n💬 [Agent Message]: ${msg.content}")
                }
            }
        }
        return response
    }
}
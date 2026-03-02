# 🚀 Coding AI Agent (Vibe Coding CLI)

터미널 환경에서 개발자와 대화하며 로컬 파일 시스템을 직접 제어하고 코드를 작성하는 **자율형 멀티 에이전트 CLI 시스템**입니다.

사용자의 지시를 받아 아키텍처를 설계하고, 코드를 작성하며, 스스로 테스트를 실행하고 에러를 수정하는 '바이브 코딩' 경험을 제공합니다.

---

## 🏗️ 핵심 아키텍처 (Multi-Agent & ReAct)

본 프로젝트는 두 가지 강력한 LLM 모델의 역할 분담을 통해 효율성과 정확성을 극대화했습니다.

1. **Phase 1: 수석 아키텍트 (Gemini 2.5 Pro)**
    * **역할:** 사용자의 요구사항을 분석하여 마크다운 형태의 논리적 설계도 작성.
    * **특징:** 기획 단계에서 단 한 번만 호출되어 전체적인 큰 그림을 그립니다.
2. **Phase 2: 실무 개발자 (Claude 3.7 Sonnet)**
    * **역할:** 설계도를 바탕으로 로컬 도구(Tool)를 사용하여 실제 코드를 구현.
    * **특징:** ReAct (Reasoning + Acting) 패턴을 통해 코드를 짜고, 테스트를 돌려보고, 에러가 발생하면 스스로 원인을 분석하여 코드를 재수정하는 자율 루프를 수행합니다.

---

## ✨ 주요 기능 및 특징

* **대화형 REPL CLI:** 무거운 웹 서버 없이 `CommandLineRunner`를 통해 터미널에서 즉각적으로 반응하는 가벼운 환경.
* **로컬 파일 제어 도구 (AgentTools):**
    * `readFile`: 파일 내용 읽기
    * `writeCodeToFile`: 코드 덮어쓰기 및 디렉토리 자동 생성
    * `executeShellCommand`: 터미널 명령어 실행 (테스트 빌드, 쉘 스크립트 등)
* **스마트 라우팅:** 입력 명령어에 따라 [설계+구현] 전체 파이프라인을 탈지, 실무자에게 [직접 구현/수정]만 지시할지 자동으로 분기.
* **안전성 및 무한 루프 방지:** * 쉘 커맨드 실행 시 **300초 타임아웃** 강제 적용.
    * API 토큰 낭비를 막기 위한 ReAct 루프 **최대 5회 시도 제한** 룰 주입.
* **투명한 로깅:** `AgentActionLoggingAdvisor`를 통해 AI가 어떤 도구를 언제 사용하는지 터미널에 실시간 출력.
* **프로젝트 룰 주입:** 작업 경로 루트에 있는 `CLAUDE.md` 파일을 읽어 에이전트의 컨텍스트에 자동 주입.

---

## 🛠️ 기술 스택

* **Language:** Kotlin 2.0.21
* **Framework:** Spring Boot 3.5.11 (Web Application Type: NONE)
* **AI Integration:** Spring AI 1.0.0-M5
    * *Note: Gemini API 통신 안정성을 위해 Spring AI의 OpenAI 호환 모드(Base URL 우회)를 사용하도록 아키텍처가 최적화되어 있습니다.*

---

## ⚙️ 글로벌 CLI 환경 세팅 (Mac/Linux 기준)

어느 프로젝트 폴더에서든 `coding-ai-agent` 명령어로 에이전트를 즉시 호출할 수 있도록 시스템 전역 환경을 세팅합니다. 프로젝트 루트 디렉토리에서 아래 3단계를 순서대로 실행하세요.

### Step 1: 빌드 및 전역 폴더 세팅
에이전트 구동에 필요한 JAR 파일을 안전한 숨김 폴더에 보관합니다.

```bash
# 1. 사용자 홈 디렉토리에 숨김 폴더 생성
mkdir -p ~/.coding-ai-agent

# 2. 프로젝트 최신 상태로 빌드 (JAR 파일 생성, 테스트 생략)
./gradlew clean build -x test

# 3. 빌드된 JAR 파일을 숨김 폴더로 복사 (이름을 agent.jar로 단순화)
cp build/libs/vibe-coding-0.0.1-SNAPSHOT.jar ~/.coding-ai-agent/agent.jar
```

### Step 2: 실행 스크립트 작성 및 API 키 등록
어느 폴더에서든 API 키를 물고 실행되도록 래퍼(Wrapper) 스크립트를 작성합니다.

```bash
# 편집기(nano)로 래퍼 스크립트 생성
nano ~/.coding-ai-agent/coding-ai-agent
```
편집기가 열리면 아래 코드를 붙여넣고, 본인의 실제 API 키로 변경한 뒤 저장(Ctrl+O, Enter)하고 종료(Ctrl+X)합니다.

```bash
#!/bin/bash

# API 키 세팅 (여기에 실제 발급받은 키를 입력하세요)
export GEMINI_API_KEY="AIzaSy..."
export CLAUDE_API_KEY="sk-ant-..."

# 스프링 부트 배너 끄기 (깔끔한 터미널 UI 유지)
export SPRING_MAIN_BANNER_MODE="off"

# JAR 파일 실행 (현재 터미널 경로를 작업 디렉토리로 자동 인식)
java -jar ~/.coding-ai-agent/agent.jar
```

### Step 3: 실행 권한 부여 및 시스템 명령어(PATH) 등록
어느 폴더에서든 API 키를 물고 실행되도록 래퍼(Wrapper) 스크립트를 작성합니다.

```bash
# 1. 스크립트에 실행(eXecute) 권한 부여
chmod +x ~/.coding-ai-agent/coding-ai-agent

# 2. 전역 명령어 폴더에 바로가기 생성 (관리자 권한 필요)
sudo ln -s ~/.coding-ai-agent/coding-ai-agent /usr/local/bin/coding-ai-agent
```

## 💻 사용 방법
원하는 프로젝트 작업 폴더로 이동한 뒤, 터미널에 아래 명령어를 입력하여 에이전트를 실행합니다.

```bash
cd /path/to/your/workspace
coding-ai-agent
```

에이전트가 실행되면 `agent>` 프롬프트가 나타납니다. 상황에 맞게 명령어를 입력하세요.

### 💡 명령어 가이드
1. **새로운 기능 기획 및 구현 (`/plan`)**
    * **입력:** `/plan 회원가입 기능을 만들고 단위 테스트를 작성해줘.
    * **동작:** Gemini가 전체 설계를 마크다운으로 작성한 뒤, Claude가 설계도를 바탕으로 코드를 구현하고 테스트를 돌립니다.
2. **직접 코드 수정 및 대화 (일반 텍스트)**
    * **입력:** `아까 만든 코드에서 비밀번호를 8자리 이상으로 검증하는 로직을 추가해줘.
    * **동작:** 무거운 기획(Gemini) 단계를 건너뛰고, Claude 실무자만 호출되어 즉시 코드를 수정하고 디버깅합니다.
3. **컨텍스트 초기화 (`clear` 또는 `reset`)**
    * **입력:** `clear`
    * **동작:** 에이전트가 가지고 있던 이전 대화 기록과 설계 문서 기억을 모두 초기화합니다. 새로운 컨텍스트로 작업을 시작할 때 유용합니다.
4. **종료 (`exit` 또는 `quit`)**
    * **동작:** 에이전트를 종료하고 일반 터미널로 돌아옵니다.
# Project 001: AI-Powered Interactive Portfolio

> An innovative voice-activated portfolio featuring a personal AI assistant that demonstrates advanced AI/ML capabilities through natural, human-like interactions.

## 🎯 Vision

Say "Hey Sreenivas" to any visitor on your portfolio website, and they'll be greeted by an AI assistant with complete knowledge of your professional background, projects, and capabilities. This digital counterpart can explain your work, provide project demos, maintain conversation context, and showcase the future of AI-human interaction.

## 📋 Table of Contents

- [System Architecture](#-system-architecture)
- [Data Flow](#-data-flow)
- [Database Design](#-database-design)
- [MCP Tools](#-mcp-tools)
- [API Structure](#-api-structure)
- [Authentication Flow](#-authentication-flow)
- [Development Timeline](#-development-timeline)
- [Installation & Setup](#-installation--setup)
- [Usage Examples](#-usage-examples)
- [Deployment](#-deployment)

## 🏗️ System Architecture

```mermaid
graph TB
    subgraph "Frontend Layer"
        UI[Next.js Portfolio UI]
        Voice[Voice Interface<br/>Wake Word + STT/TTS]
        Auth[Supabase Auth]
    end
    
    subgraph "API Gateway Layer"
        Gateway[FastAPI Gateway]
        AuthMW[Auth Middleware]
        CostMW[Cost Monitor]
        RateLimit[Rate Limiter]
    end
    
    subgraph "Core Services Layer"
        Orchestrator[MCP Orchestrator]
        RAG[RAG Service]
        Memory[Memory Engine]
        Tools[MCP Tools]
    end
    
    subgraph "Data Layer"
        PostgresDB[(PostgreSQL + pgvector)]
        VectorStore[Vector Embeddings]
        Sessions[Session Storage]
    end
    
    subgraph "External Services"
        OpenAI[OpenAI API]
        Supabase[Supabase Backend]
    end
    
    UI --> Gateway
    Voice --> Gateway
    Auth --> Supabase
    
    Gateway --> AuthMW
    AuthMW --> CostMW
    CostMW --> RateLimit
    RateLimit --> Orchestrator
    
    Orchestrator --> RAG
    Orchestrator --> Memory
    Orchestrator --> Tools
    
    RAG --> PostgresDB
    Memory --> PostgresDB
    Tools --> PostgresDB
    
    Orchestrator --> OpenAI
    Gateway --> Supabase
```

## 🔄 Data Flow

### User Interaction Flow

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Gateway
    participant Orchestrator
    participant Tools
    participant Database
    participant LLM
    
    User->>Frontend: "Hey Sreenivas, tell me about your projects"
    Frontend->>Gateway: WebSocket + Bearer Token
    Gateway->>Gateway: Validate JWT + Check Rate Limits
    Gateway->>Orchestrator: Authenticated Request
    
    Orchestrator->>Tools: user_history_tool(user_id)
    Tools->>Database: Query past conversations
    Database-->>Tools: Conversation history
    Tools-->>Orchestrator: User context
    
    Orchestrator->>Tools: knowledge_search_tool(query)
    Tools->>Database: Vector similarity search
    Database-->>Tools: Relevant project data
    Tools-->>Orchestrator: Project information
    
    Orchestrator->>LLM: Generate response with context
    LLM-->>Orchestrator: AI response
    
    Orchestrator->>Tools: save_conversation_tool()
    Tools->>Database: Store turn + update summary
    
    Orchestrator-->>Gateway: Response + metadata
    Gateway-->>Frontend: Streaming response
    Frontend-->>User: Voice + Text output
```

### Memory Management Flow

```mermaid
graph LR
    subgraph "Conversation Turn"
        Input[User Input]
        Process[Process + Respond]
        Store[Store Turn]
    end
    
    subgraph "Every 10 Turns"
        Check{Turn Count<br/>% 10 == 0?}
        Summarize[Generate Summary]
        UpdateContext[Update Context]
    end
    
    subgraph "Session End"
        SessionEnd[Session Ends]
        LongTermSummary[Update Long-term<br/>User Profile]
        ExtractMemory[Extract Memory Items]
    end
    
    Input --> Process
    Process --> Store
    Store --> Check
    Check -->|Yes| Summarize
    Check -->|No| Input
    Summarize --> UpdateContext
    UpdateContext --> Input
    
    Process --> SessionEnd
    SessionEnd --> LongTermSummary
    SessionEnd --> ExtractMemory
```

## 🗄️ Database Design

```mermaid
erDiagram
    users {
        uuid id PK
        string email
        string role
        timestamp last_seen_at
        timestamp created_at
    }
    
    user_profiles {
        uuid user_id PK
        string display_name
        text long_term_summary
        jsonb preferences
        timestamp updated_at
    }
    
    sessions {
        uuid id PK
        uuid user_id FK
        string ip_hash
        string user_agent
        timestamp started_at
        timestamp ended_at
    }
    
    turns {
        uuid id PK
        uuid session_id FK
        string role
        text content
        integer tokens_used
        timestamp created_at
    }
    
    session_summaries {
        uuid id PK
        uuid session_id FK
        integer version
        text summary
        timestamp created_at
    }
    
    documents {
        uuid id PK
        string uri
        string title
        string kind
        string checksum
        timestamp created_at
    }
    
    chunks {
        uuid id PK
        uuid document_id FK
        integer ordinal
        text content
        vector embedding
    }
    
    user_memory_items {
        uuid id PK
        uuid user_id FK
        string kind
        string key
        string value
        float confidence
        uuid source_turn_id FK
        timestamp created_at
    }
    
    cost_events {
        uuid id PK
        uuid session_id FK
        string provider
        string model
        integer prompt_tokens
        integer completion_tokens
        decimal usd_cost
        timestamp created_at
    }
    
    tool_invocations {
        uuid id PK
        uuid session_id FK
        string tool_name
        jsonb args
        jsonb result
        timestamp created_at
    }
    
    users ||--|| user_profiles : has
    users ||--o{ sessions : creates
    sessions ||--o{ turns : contains
    sessions ||--o{ session_summaries : summarized_by
    users ||--o{ user_memory_items : remembers
    sessions ||--o{ cost_events : incurs
    sessions ||--o{ tool_invocations : executes
    documents ||--o{ chunks : split_into
    turns ||--o{ user_memory_items : sources
```

## 🛠️ MCP Tools

```mermaid
graph TB
    subgraph "MCP Orchestrator"
        Orchestrator[Central Decision Engine]
    end
    
    subgraph "Core Tools"
        ProfileTool[profile_tool<br/>- get_intro<br/>- get_timeline<br/>- get_skills]
        ProjectsTool[projects_tool<br/>- list_projects<br/>- get_details<br/>- get_demo]
        KnowledgeTool[knowledge_tool<br/>- search_rag<br/>- get_citations<br/>- find_similar]
    end
    
    subgraph "Memory Tools"
        UserMemoryTool[user_memory_tool<br/>- get_history<br/>- update_context<br/>- personalized_greeting]
        ConversationTool[conversation_tool<br/>- save_turn<br/>- create_summary<br/>- get_context]
    end
    
    subgraph "Utility Tools"
        DemoTool[demo_tool<br/>- open_modal<br/>- show_code<br/>- live_preview]
        QueryTool[query_tool<br/>- nl_to_sql<br/>- describe_schema<br/>- safe_execute]
    end
    
    Orchestrator --> ProfileTool
    Orchestrator --> ProjectsTool
    Orchestrator --> KnowledgeTool
    Orchestrator --> UserMemoryTool
    Orchestrator --> ConversationTool
    Orchestrator --> DemoTool
    Orchestrator --> QueryTool
```

### Tool Implementation Pattern

```python
from fastapi_mcp import mcp_tool

@mcp_tool(name="profile_get_intro", description="Get Sreenivas's introduction and background")
def get_profile_intro() -> dict:
    return {
        "name": "Sreenivas Gurram",
        "tagline": "AI/ML Engineer building the future",
        "background": "From Ballari to BMS College, now building The LSG Group",
        "current_roles": ["Data Scientist at digitalapi.ai", "Team Lead at Rightsense", "AI Engineer at Skooc"]
    }

@mcp_tool(name="user_memory_fetch", description="Fetch user conversation history and context")
def fetch_user_memory(user_id: str, limit: int = 5) -> dict:
    with get_db_session() as session:
        summaries = session.query(SessionSummary).filter_by(user_id=user_id).limit(limit).all()
        return {
            "has_history": len(summaries) > 0,
            "greeting": generate_personalized_greeting(summaries),
            "context": [s.summary for s in summaries]
        }
```

## 🔌 API Structure

```mermaid
graph TB
    subgraph "Public Endpoints"
        Auth[POST /auth/login<br/>POST /auth/register<br/>POST /auth/refresh]
        Chat[POST /chat<br/>WebSocket /ws]
    end
    
    subgraph "User Endpoints (Bearer Token)"
        UserAPI[GET /api/me/profile<br/>GET /api/me/sessions<br/>GET /api/me/export<br/>DELETE /api/me/forget]
    end
    
    subgraph "Admin Endpoints (Admin Token)"
        AdminAPI[GET /admin/analytics<br/>GET /admin/costs<br/>GET /admin/users<br/>POST /admin/api-keys]
    end
    
    subgraph "MCP Internal (Internal Token)"
        MCPEndpoints[POST /mcp/profile/*<br/>POST /mcp/projects/*<br/>POST /mcp/memory/*<br/>POST /mcp/conversation/*]
    end
    
    Gateway[API Gateway] --> Auth
    Gateway --> Chat
    Gateway --> UserAPI
    Gateway --> AdminAPI
    Gateway --> MCPEndpoints
```

## 🔐 Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Supabase
    participant Gateway
    participant Database
    
    User->>Frontend: Visit portfolio
    Frontend->>Frontend: Check local storage for token
    
    alt No existing token
        Frontend->>Supabase: Create anonymous session
        Supabase-->>Frontend: Anonymous JWT
        Frontend->>Database: Create anonymous user record
    else Existing valid token
        Frontend->>Gateway: Validate token
        Gateway->>Supabase: Verify JWT
        Supabase-->>Gateway: Token valid + user info
        Gateway->>Database: Update last_seen_at
    end
    
    User->>Frontend: "Hey Sreenivas" (voice activation)
    Frontend->>Gateway: Chat request + Bearer token
    Gateway->>Gateway: Validate JWT + rate limits
    Gateway->>Database: Load user context
    Gateway-->>Frontend: Personalized response
```

## 📅 Development Timeline

```mermaid
gantt
    title Project 001 Development Schedule
    dateFormat  YYYY-MM-DD
    section Foundation
    Repository Setup           :done, setup, 2024-08-10, 2d
    Docker Environment        :done, docker, after setup, 3d
    Database Schema           :active, db, after docker, 4d
    
    section Backend Core
    FastAPI Gateway           :gateway, after db, 5d
    Authentication System     :auth, after gateway, 3d
    MCP Integration          :mcp, after auth, 4d
    RAG Implementation       :rag, after mcp, 3d
    
    section Frontend
    Next.js Setup            :frontend, after auth, 4d
    Voice Interface          :voice, after frontend, 3d
    UI Components            :ui, after voice, 3d
    
    section Integration
    End-to-End Testing       :testing, after rag, 4d
    Performance Optimization :perf, after testing, 2d
    Documentation           :docs, after perf, 2d
    
    section Deployment
    Production Setup         :prod, after docs, 3d
    Domain & SSL            :ssl, after prod, 1d
    Launch                  :launch, after ssl, 1d
```

## 🚀 Installation & Setup

### Prerequisites

- Docker & Docker Compose
- Node.js 18+
- Python 3.11+
- PostgreSQL with pgvector extension

### Quick Start

```bash
# Clone the repository
git clone https://github.com/LSG-hub/Project-001.git
cd Project-001

# Copy environment variables
cp .env.example .env
# Edit .env with your API keys and database credentials

# Start all services
docker-compose up -d

# Install dependencies
cd apps/portfolio && npm install
cd ../assistant && pip install -r requirements.txt

# Run database migrations
make migrate

# Start development servers
make dev
```

### Environment Variables

```bash
# Database
DATABASE_URL=postgresql://user:password@localhost:5432/project001

# Authentication
SUPABASE_URL=your_supabase_url
SUPABASE_ANON_KEY=your_supabase_anon_key
JWT_SECRET=your_jwt_secret

# AI Services
OPENAI_API_KEY=your_openai_key

# Optional
ELEVENLABS_API_KEY=your_elevenlabs_key
```

### Docker Compose

```yaml
version: '3.8'
services:
  db:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: project001
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./infra/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
  
  assistant:
    build: ./apps/assistant
    depends_on:
      - db
    environment:
      DATABASE_URL: postgresql://user:password@db:5432/project001
    ports:
      - "8000:8000"
    volumes:
      - ./content:/app/content
  
  portfolio:
    build: ./apps/portfolio
    depends_on:
      - assistant
    ports:
      - "3000:3000"
    environment:
      NEXT_PUBLIC_API_URL: http://localhost:8000

volumes:
  postgres_data:
```

## 💬 Usage Examples

### Basic Conversation

```javascript
// Frontend WebSocket connection
const ws = new WebSocket('ws://localhost:8000/ws');
ws.send(JSON.stringify({
    message: "Tell me about Sreenivas's background",
    token: "bearer_token_here"
}));

// Expected response structure
{
    "response": "I'm Sreenivas's AI assistant. He grew up in Ballari, Karnataka...",
    "context": {
        "user_id": "uuid",
        "session_id": "uuid",
        "tokens_used": 150,
        "sources": ["profile.yaml", "education.md"]
    }
}
```

### Voice Activation

```javascript
// Wake word detection
const porcupine = await PorcupineWeb.create(
    'your_access_key',
    ['/path/to/hey_sreenivas.ppn']
);

// Speech recognition
const recognition = new webkitSpeechRecognition();
recognition.onresult = (event) => {
    const transcript = event.results[0][0].transcript;
    sendToAssistant(transcript);
};
```

### MCP Tool Call

```python
# Backend tool invocation
@mcp_tool(name="projects_list", description="List all projects")
async def list_projects(category: str = "all") -> dict:
    async with get_db_session() as session:
        projects = await session.execute(
            select(Project).where(Project.category == category if category != "all" else True)
        )
        return {
            "projects": [
                {
                    "id": p.id,
                    "name": p.name,
                    "description": p.description,
                    "tech_stack": p.tech_stack,
                    "status": p.status
                } for p in projects.scalars().all()
            ]
        }
```

## 🚀 Deployment

### Production Architecture

```mermaid
graph TB
    subgraph "CDN Layer"
        CloudFlare[CloudFlare CDN]
    end
    
    subgraph "Frontend"
        Vercel[Vercel<br/>Next.js App]
    end
    
    subgraph "Backend"
        Railway[Railway<br/>FastAPI + MCP]
    end
    
    subgraph "Database"
        SupabaseDB[Supabase<br/>PostgreSQL + pgvector]
    end
    
    subgraph "External Services"
        OpenAI[OpenAI API]
        ElevenLabs[ElevenLabs TTS]
    end
    
    CloudFlare --> Vercel
    Vercel --> Railway
    Railway --> SupabaseDB
    Railway --> OpenAI
    Railway --> ElevenLabs
```

### Deployment Commands

```bash
# Frontend (Vercel)
cd apps/portfolio
vercel --prod

# Backend (Railway)
cd apps/assistant
railway up

# Database migrations
railway run python -m alembic upgrade head
```

## 📊 Monitoring & Analytics

### Cost Monitoring

```python
# Cost tracking middleware
@app.middleware("http")
async def cost_tracking_middleware(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    
    # Log cost information
    await log_usage(
        session_id=request.state.session_id,
        endpoint=request.url.path,
        tokens_used=request.state.tokens_used,
        response_time=time.time() - start_time
    )
    
    return response
```

### Analytics Dashboard

- User engagement metrics
- Conversation flow analysis
- Cost per session tracking
- Popular topics and queries
- Voice vs text interaction ratios

## 🔧 Development Commands

```bash
# Database operations
make migrate          # Run database migrations
make seed            # Seed with initial data
make reset-db        # Reset database

# Development
make dev             # Start all development servers
make test            # Run test suite
make lint            # Code linting
make format          # Code formatting

# Deployment
make build           # Build for production
make deploy          # Deploy to production
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with collaboration between Claude, GPT-4, Gemini, and Grok
- Inspired by the vision of democratizing AI interaction
- Part of the journey toward building The LSG Group

---

*"From a simple voice command to a complete AI interaction - showcasing the future of human-AI collaboration."*

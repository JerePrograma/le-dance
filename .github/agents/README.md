# AI-Powered Spec-Driven Development Prompts

A comprehensive collection of prompts designed for GitHub Copilot and other coding agents to facilitate structured, approval-gated software development workflows. This system transforms high-level ideas into formal specifications, technical designs, and implementation plans following industry best practices.

## Role

This project adheres to the **Linus Torvalds Development Guidelines** as defined in `design-principle.instructions.md`:

### Core Principles
- **Collaboration First**: Absolutely no implementation without explicit user approval
- **Keep It Simple, Stupid (KISS)**: Avoid over-engineering and unnecessary complexity
- **English for Code**: All code, comments, and documentation must be in English for maintainability
- **Pragmatic Solutions**: Favor obviously correct code over clever tricks
- **Maintainability**: Long-term convenience over short-term hacks

The development approach emphasizes directness, pragmatism, and "good taste" in code, ensuring every abstraction justifies its existence and complexity is only introduced when it solves real problems.

## Kiro Workflow

The system implements a structured, four-phase spec-driven development workflow with mandatory approval gates:

### `/createSpec`

**Requirements Generation Phase**

Transforms high-level feature ideas into formal, unambiguous requirements documents using industry standards.

**Key Features:**
- Generates `requirements.md` in structured `.kiro/specs/[feature-name]/` directories
- Uses User Stories format: "As a [role], I want [feature], so that [benefit]"
- Implements EARS (Easy Approach to Requirements Syntax) for acceptance criteria
- Incorporates project context from `.kiro/steering/` directory
- Enforces explicit approval gates before proceeding

**Workflow:**
1. Receive high-level feature request
2. Generate comprehensive first draft with scenarios and edge cases
3. Wait for explicit user approval
4. Iterate based on feedback until approved

### `/design`

**Technical Design Phase**

Converts approved requirements into comprehensive technical blueprints.

**Key Features:**
- Creates detailed `design.md` technical implementation plans
- Synthesizes requirements, project context, and existing codebase analysis
- Includes architectural overview, data flow diagrams, and component definitions
- Specifies API endpoints, database schema changes, and security considerations
- Outlines comprehensive test strategy

**Components:**
- **Architectural Overview**: High-level solution integration
- **Data Flow Diagrams**: Mermaid.js visualizations
- **Interface Definitions**: TypeScript types and component specs
- **API Contracts**: Complete endpoint specifications
- **Security Analysis**: Risk assessment and mitigation strategies
- **Test Strategy**: Unit, integration, and E2E test planning

### `/createTask`

**Implementation Planning Phase**

Decomposes approved technical designs into executable task hierarchies.

**Key Features:**
- Generates hierarchical `tasks.md` implementation checklists
- Creates numbered high-level goals with indented sub-tasks
- Ensures logical ordering and dependency management
- Provides full traceability to requirements and acceptance criteria
- No approval required - signals readiness for implementation

**Structure:**
- High-level tasks: `- [ ] 1. Goal description`
- Sub-tasks: Indented bullet points with specific actions
- Traceability: `- _Requirements: [requirement numbers]_`

### `/executeTask`

**Implementation Execution Phase**

Active development phase with autonomous progression and user control.

**Key Features:**
- Interactive command-driven workflow (`implement`, `continue`, `implement 3`)
- Mandatory comprehensive context gathering before coding
- Full document verification and comprehension summaries
- Implementation planning with architecture relationship analysis
- Automatic progress tracking and task completion marking

**Mandatory Process:**
1. **Context Gathering**: Read all steering, design, and requirements documents
2. **Comprehension Verification**: Summarize understanding and connections
3. **Implementation Planning**: Explain approach and file modifications
4. **Execution**: Code implementation following all specifications
5. **Progress Tracking**: Update task status and report completion

## Tools

### Version Control & Code Quality

#### `/commit`
**Professional Git Commit Assistant**

Analyzes changes and creates professional commit messages following best practices.

**Features:**
- Categorizes file types and evaluates change patterns
- Filters out internal development artifacts
- Determines appropriate commit strategy
- Generates conventional commit messages
- Identifies breaking changes and dependencies

#### `/prReview`
**Comprehensive Code Review Assistant**

Conducts in-depth Pull Request reviews simulating senior developer expertise.

**Prerequisites:**
- GitHub CLI (`gh`) must be installed and authenticated
- Install: `brew install gh` (macOS) or visit https://cli.github.com/
- Authenticate: `gh auth login`

**Capabilities:**
- Fetches and analyzes PR metadata using GitHub CLI
- Performs comprehensive change analysis
- Identifies risks, security issues, and architectural concerns
- Provides specific, actionable feedback
- Evaluates testing coverage and documentation quality

## Usage

Each prompt is designed to work independently or as part of the complete workflow:

1. **Start with `/createSpec`** for new features
2. **Progress to `/design`** after requirements approval
3. **Use `/createTask`** to plan implementation
4. **Execute with `/executeTask`** for development
5. **Support with `/commit`** and `/prReview`** for quality assurance

All prompts enforce the collaboration-first principle, ensuring human oversight at critical decision points while maintaining development velocity through structured automation.

## References

- [GitHub Awesome Copilot](https://github.com/github/awesome-copilot) - Curated list of GitHub Copilot resources, prompts, and best practices *(Note: This project was developed independently without reference to this repository)*

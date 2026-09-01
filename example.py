import asyncio
from dotenv import load_dotenv
from google.antigravity import Agent, CapabilitiesConfig, LocalAgentConfig

load_dotenv()


async def main() -> None:
    config = LocalAgentConfig(
        model="gemini-3.6-flash",
        system_instructions="You are an expert assistant for codebase navigation.",
        capabilities=CapabilitiesConfig(),
    )
    async with Agent(config) as agent:
        response = await agent.chat("Hello!")
        async for token in response:
            print(token, end="", flush=True)
        print()


if __name__ == "__main__":
    asyncio.run(main())

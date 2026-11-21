# TOB — AI Portfolio Chatbot

An AI-powered portfolio chatbot built with Java, Spring Boot, and LangChain4j.

TOB (The Online Buddy) helps visitors explore my professional experience, technical skills, projects, and career journey through natural-language conversations.

**Live Portfolio:** [Visit Website](https://jimichaela18.github.io/israelin-michaela-portfolio/)

## Overview

The application exposes a REST API that receives questions from the portfolio frontend and generates answers using an LLM with information about my professional background.

It supports two approaches to providing portfolio knowledge:

- **Full-context prompting:** Supplies the portfolio information as context to the language model.
- **Retrieval-Augmented Generation (RAG):** Retrieves relevant portfolio information before generating an answer.

## Technologies

- Java 21
- Spring Boot 3.5.7
- Maven
- LangChain4j
- Groq LLM
- All-MiniLM embeddings
- In-memory vector store
- Docker
- Render

## Deployment

The backend is deployed on Render.

**Backend URL:** [https://portfolio-chatbot-yoaz.onrender.com/](https://portfolio-chatbot-yoaz.onrender.com/)

**Health Check:** [https://portfolio-chatbot-yoaz.onrender.com/health](https://portfolio-chatbot-yoaz.onrender.com/health)

The application uses Docker for deployment.

## Related Project

**Frontend:** [israelin-michaela-portfolio](https://github.com/JIMichaela18/israelin-michaela-portfolio)

The frontend is hosted on GitHub Pages and communicates with this backend to power TOB.

---

**Author:** Israelin Michaela

[GitHub](https://github.com/JIMichaela18) · [LinkedIn](https://www.linkedin.com/in/israelin-michaela-joseph-b088241b0/)

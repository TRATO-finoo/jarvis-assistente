export default async (req) => {
  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ error: "Use POST." }),
      {
        status: 405,
        headers: { "Content-Type": "application/json" }
      }
    );
  }

  try {
    const body = await req.json();
    const mensagem = body?.mensagem;

    if (!mensagem || typeof mensagem !== "string") {
      return new Response(
        JSON.stringify({ error: "Mensagem não enviada." }),
        {
          status: 400,
          headers: { "Content-Type": "application/json" }
        }
      );
    }

    const apiKey = Netlify.env.get("OPENAI_API_KEY");

    if (!apiKey) {
      return new Response(
        JSON.stringify({
          error: "OPENAI_API_KEY não configurada no Netlify."
        }),
        {
          status: 500,
          headers: { "Content-Type": "application/json" }
        }
      );
    }

    const resposta = await fetch(
      "https://api.openai.com/v1/responses",
      {
        method: "POST",

        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`
        },

        body: JSON.stringify({
          model: "gpt-5.6-sol",

          instructions:
            "Você é Jarvis, um assistente pessoal inteligente. " +
            "Responda em português do Brasil. " +
            "Seja natural, útil e objetivo. " +
            "Quando o usuário pedir uma ação no celular, " +
            "explique claramente o que deve ser feito.",

          input: mensagem,

          max_output_tokens: 1200
        })
      }
    );

    const dados = await resposta.json();

    if (!resposta.ok) {
      return new Response(
        JSON.stringify({
          error: dados?.error?.message || "Erro na OpenAI."
        }),
        {
          status: resposta.status,
          headers: { "Content-Type": "application/json" }
        }
      );
    }

    const texto =
      dados?.output
        ?.flatMap(item => item.content || [])
        ?.find(item => item.type === "output_text")
        ?.text || "Não consegui gerar uma resposta.";

    return new Response(
      JSON.stringify({
        resposta: texto
      }),
      {
        status: 200,
        headers: {
          "Content-Type": "application/json"
        }
      }
    );

  } catch (erro) {

    return new Response(
      JSON.stringify({
        error: "Erro interno no servidor.",
        detalhe: erro?.message || String(erro)
      }),
      {
        status: 500,
        headers: {
          "Content-Type": "application/json"
        }
      }
    );
  }
};

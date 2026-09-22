export default async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "POST, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type"
      }
    });
  }

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ error: "Método não permitido." }),
      {
        status: 405,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*"
        }
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
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*"
          }
        }
      );
    }

    const apiKey = process.env.OPENAI_API_KEY;

    if (!apiKey) {
      return new Response(
        JSON.stringify({
          error: "OPENAI_API_KEY ainda não foi configurada no Netlify."
        }),
        {
          status: 500,
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*"
          }
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
            "Responda sempre em português do Brasil. " +
            "Seja natural, inteligente, útil e objetivo. " +
            "Não invente informações.",
          input: mensagem,
          max_output_tokens: 1200
        })
      }
    );

    const dados = await resposta.json();

    if (!resposta.ok) {
      return new Response(
        JSON.stringify({
          error:
            dados?.error?.message ||
            "A OpenAI retornou um erro."
        }),
        {
          status: resposta.status,
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*"
          }
        }
      );
    }

    let texto = "";

    if (Array.isArray(dados?.output)) {
      for (const item of dados.output) {
        if (Array.isArray(item?.content)) {
          for (const content of item.content) {
            if (
              content?.type === "output_text" &&
              typeof content?.text === "string"
            ) {
              texto += content.text;
            }
          }
        }
      }
    }

    if (!texto) {
      texto = "Não consegui gerar uma resposta.";
    }

    return new Response(
      JSON.stringify({ resposta: texto }),
      {
        status: 200,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*"
        }
      }
    );

  } catch (erro) {
    return new Response(
      JSON.stringify({
        error: "Erro interno no servidor."
      }),
      {
        status: 500,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*"
        }
      }
    );
  }
};

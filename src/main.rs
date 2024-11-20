use anyhow::Result;
use log::{error, info};
use reqwest::{
    header::{HeaderMap, HeaderValue, ACCEPT, CONTENT_TYPE, USER_AGENT},
    Client,
};
use serde_json::json;

async fn fetch_token() -> Result<Option<String>> {
    let url = "https://duckduckgo.com/duckchat/v1/status";

    let mut headers = HeaderMap::new();
    headers.insert(
        USER_AGENT,
        HeaderValue::from_static(
            "Mozilla/5.0 (X11; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0",
        ),
    );
    headers.insert(ACCEPT, HeaderValue::from_static("*/*"));
    headers.insert(
        "Referer",
        HeaderValue::from_static("https://duckduckgo.com/"),
    );
    headers.insert("x-vqd-accept", HeaderValue::from_static("1"));
    headers.insert("Connection", HeaderValue::from_static("keep-alive"));
    headers.insert(
        "Cookie",
        HeaderValue::from_static("dcm=5; ah=it-it; l=wt-wt"),
    );

    let client = Client::new();
    let response = client.head(url).headers(headers).send().await?;

    if response.status().is_success() {
        if let Some(token) = response.headers().get("x-vqd-4") {
            let token_str = token.to_str()?;
            info!("Token fetched: {}", token_str);
            return Ok(Some(token_str.to_string()));
        }
    } else {
        error!("Failed to fetch token. Status code: {}", response.status());
    }
    Ok(None)
}

async fn fetch_messages(token: Option<String>, prompt: &str) -> Result<()> {
    if token.is_none() {
        error!("No token provided.");
        return Ok(());
    }

    let url = "https://duckduckgo.com/duckchat/v1/chat";
    let mut headers = HeaderMap::new();
    headers.insert(
        USER_AGENT,
        HeaderValue::from_static(
            "Mozilla/5.0 (X11; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0",
        ),
    );
    headers.insert(ACCEPT, HeaderValue::from_static("text/event-stream"));
    headers.insert(CONTENT_TYPE, HeaderValue::from_static("application/json"));
    headers.insert("x-vqd-4", HeaderValue::from_str(token.as_ref().unwrap())?);
    headers.insert("Sec-GPC", HeaderValue::from_static("1"));

    let body = json!({
        "model": "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo",
        "messages": [{"role": "user", "content": prompt}],
    });

    let client = Client::new();
    let mut response = client.post(url).headers(headers).json(&body).send().await?;

    if !response.status().is_success() {
        error!("Request failed with status: {}", response.status());
        return Ok(());
    }

    println!("Reply:");
    let mut full_response = String::new(); // Accumulate the full response here
    while let Some(chunk) = response.chunk().await? {
        let chunk_str = String::from_utf8_lossy(&chunk);
        for line in chunk_str.lines() {
            if line.trim_start().starts_with("data:") {
                let data_str = &line["data:".len()..].trim();
                let data: serde_json::Value = match serde_json::from_str(data_str) {
                    Ok(d) => d,
                    Err(_) => continue,
                };

                if let Some(message) = data.get("message") {
                    // Remove quotes and trim whitespace
                    let message_str = message.as_str().unwrap_or("").trim_matches('"').trim();
                    if !message_str.is_empty() {
                        if !full_response.is_empty() {
                            // Check if the last character is not a space or punctuation
                            if !full_response.ends_with(' ')
                                && !full_response.ends_with('.')
                                && !full_response.ends_with(',')
                            {
                                full_response.push(' '); // Add a space before the next message
                            }
                        }
                        full_response.push_str(message_str); // Append the message to the full response
                    }
                }
            }
        }
    }

    println!("{}", full_response);
    Ok(())
}

#[tokio::main]
async fn main() -> Result<()> {
    env_logger::init();
    let prompt = {
        let mut input = String::new();
        println!("Enter a prompt: ");
        std::io::stdin().read_line(&mut input)?;
        input.trim().to_string()
    };

    let token = fetch_token().await?;
    fetch_messages(token, &prompt).await?;
    Ok(())
}

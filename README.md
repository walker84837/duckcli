# DuckCLI

> reimplemntation of duck.ai as a CLI utility

DuckCLI is a clean room implementation of Duck.ai's API endpoint, specifically designed to interact with DuckDuckGo AI Chat without the need for using the browser.

## Features

* Fetches a token from the DuckDuckGo status endpoint
* Sends a prompt to the DuckDuckGo chat endpoint using the fetched token
* Processes the raw string data from the chat endpoint and extracts the response message

## Usage

1. Clone the repository and navigate to the project directory.
2. Compile the project using `sbt compile`.
3. Run the project using `sbt run`.
4. Enter a prompt when prompted to receive a response from DuckDuckGo AI Chat.

## Notes

* As this is a clean room implementation, this project is not affiliated with DuckDuckGo.
* The implementation is based on the publicly available API endpoint (as far as I could see with developer tools).
* The project uses a hardcoded User-Agent header to mimic a Firefox browser, though this may be any browser you like: I found that using user agents like cURL doesn't give the needed token.

## Dependencies

* Sttp client library (version 3.x)
* Circe JSON library (version 0.14.x)
* Scala (version 2.13.x)

## License

This project is licensed under the Apache v2 License. See the [LICENSE](LICENSE) file for details.

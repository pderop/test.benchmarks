# Servers shootout board
## Results

| Application  | Remote | TextPlain | Trends |
| ---  | :---: | :---: | :---: |
| benchmark-rn-11x-H1S | [**24589.15**](bench/benchmark-rn-11x-H1S/Remote/index.html) | [****](bench/benchmark-rn-11x-H1S/TextPlain/index.html) | [**result**](bench/benchmark-rn-11x-H1S/Trends/index.html) |
| benchmark-rn-11x-H2 | [**22733.1**](bench/benchmark-rn-11x-H2/Remote/index.html) | [****](bench/benchmark-rn-11x-H2/TextPlain/index.html) | [**result**](bench/benchmark-rn-11x-H2/Trends/index.html) |
| benchmark-rn-concurrent-H2 | [**43250.933**](bench/benchmark-rn-concurrent-H2/Remote/index.html) | [**82919.65**](bench/benchmark-rn-concurrent-H2/TextPlain/index.html) | [**result**](bench/benchmark-rn-concurrent-H2/Trends/index.html) |
| benchmark-rs | [**86764.417**](bench/benchmark-rs/Remote/index.html) | n/a | [**result**](bench/benchmark-rs/Trends/index.html) |

## Scenario

Each benchmark case starts with no traffic and does the following:

- increase the concurrency by 100 users (1 users = 1 connection) in 1 seconds
- hold that concurrency level for 1 seconds
- go to first step, unless the maximum concurrency of 1000 users is reached

## Benchmark cases
- PlainText: frontend responds a "text/plain" response body
- Echo: gatling sends a "text/plain" request body and the frontend echoes that in the response
- JsonGet: frontend responds with a JSON payload
- JsonPost: gatling sends a JSON payload, frontend deserializes it and replies with a JSON payload
- HtmlGet: front renders an HTML view with a templating engine
- Remote: frontend forwards gatling request to the backend


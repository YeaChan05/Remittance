### before
```bash
LOLOAD_MAX_DURATION=30m \
BASE_URL=http://localhost:8080 \
API_VERSION=v1 \
AUTH_INTERNAL_TOKEN=remittance-internal-token \
TEST_ID=fan-in-transfer-100users-100k \
SENDER_COUNT=100 \
LOAD_VUS=50 \
LOAD_ITERATIONS=100000 \
INITIAL_DEPOSIT=1200000 \
TRANSFER_AMOUNT=1000 \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/fan-in-transfer-to-one-account.js

         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 


     execution: local
        script: /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/fan-in-transfer-to-one-account.js
        output: Prometheus remote write (http://localhost:9090/api/v1/write)

     scenarios: (100.00%) 1 scenario, 50 max VUs, 30m30s max duration (incl. graceful stop):
              * default: 100000 iterations shared among 50 VUs (maxDuration: 30m0s, gracefulStop: 30s)



  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<2000' p(95)=840.33ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 901208  575.220132/s
    checks_succeeded...: 100.00% 901208 out of 901208
    checks_failed......: 0.00%   0 out of 901208

    ✓ signup status is 200
    ✓ signup returns name
    ✓ login status is 200
    ✓ login returns accessToken
    ✓ create_account status is 200
    ✓ create_account returns accountId
    ✓ issue_idempotency_key status is 200
    ✓ idempotency key returned
    ✓ deposit status is 200
    ✓ deposit succeeded
    ✓ query_account_snapshot status is 200
    ✓ account snapshot returned
    ✓ transfer status is 200
    ✓ transfer succeeded
    ✓ transfer has id

    CUSTOM
    idempotency_key_req_duration..........: avg=9.12ms   min=1.53ms  med=7.2ms    max=4.32s p(90)=12.89ms  p(95)=16.31ms 
    internal_account_query_req_duration...: avg=6.04ms   min=928µs   med=4.76ms   max=4.31s p(90)=9.3ms    p(95)=11.64ms 
    transfer_post_req_duration............: avg=752.97ms min=38.19ms med=681.48ms max=5.19s p(90)=990.56ms p(95)=1.2s    

    HTTP
    http_req_duration.....................: avg=193.3ms  min=928µs   med=7.02ms   max=5.19s p(90)=720.15ms p(95)=840.33ms
      { expected_response:true }..........: avg=193.3ms  min=928µs   med=7.02ms   max=5.19s p(90)=720.15ms p(95)=840.33ms
    http_req_failed.......................: 0.00%  0 out of 400604
    http_reqs.............................: 400604 255.696228/s

    EXECUTION
    iteration_duration....................: avg=774.87ms min=73.17ms med=701.54ms max=5.22s p(90)=1.02s    p(95)=1.24s   
    iterations............................: 100000 63.827677/s
    vus...................................: 44     min=0           max=50
    vus_max...............................: 50     min=50          max=50

    NETWORK
    data_received.........................: 152 MB 97 kB/s
    data_sent.............................: 136 MB 87 kB/s




running (26m06.7s), 00/50 VUs, 100000 complete and 0 interrupted iterations
default ✓ [======================================] 50 VUs  25m50.1s/30m0s  100000/100000 shared iters

```


### after
```bash
LOLOAD_MAX_DURATION=30m \
BASE_URL=http://localhost:8080 \
API_VERSION=v1 \
AUTH_INTERNAL_TOKEN=remittance-internal-token \
TEST_ID=fan-in-transfer-100users-100k \
SENDER_COUNT=100 \
LOAD_VUS=50 \
LOAD_ITERATIONS=100000 \
INITIAL_DEPOSIT=1200000 \
TRANSFER_AMOUNT=1000 \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/fan-in-transfer-to-one-account.js

         /\      Grafana   /‾‾/
    /\  /  \     |\  __   /  /
   /  \/    \    | |/ /  /   ‾‾\
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/


     execution: local
        script: /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/fan-in-transfer-to-one-account.js
        output: Prometheus remote write (http://localhost:9090/api/v1/write)

     scenarios: (100.00%) 1 scenario, 50 max VUs, 30m30s max duration (incl. graceful stop):
              * default: 100000 iterations shared among 50 VUs (maxDuration: 30m0s, gracefulStop: 30s)



  █ THRESHOLDS

    http_req_duration
    ✓ 'p(95)<2000' p(95)=784.83ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS

    checks_total.......: 901208  622.245531/s
    checks_succeeded...: 100.00% 901208 out of 901208
    checks_failed......: 0.00%   0 out of 901208

    ✓ signup status is 200
    ✓ signup returns name
    ✓ login status is 200
    ✓ login returns accessToken
    ✓ create_account status is 200
    ✓ create_account returns accountId
    ✓ issue_idempotency_key status is 200
    ✓ idempotency key returned
    ✓ deposit status is 200
    ✓ deposit succeeded
    ✓ query_account_snapshot status is 200
    ✓ account snapshot returned
    ✓ transfer status is 200
    ✓ transfer succeeded
    ✓ transfer has id

    CUSTOM
    idempotency_key_req_duration..........: avg=8.31ms   min=1.5ms    med=6.59ms  max=2.62s p(90)=12.13ms  p(95)=15.48ms
    internal_account_query_req_duration...: avg=5.48ms   min=905µs    med=4.29ms  max=3.09s p(90)=8.61ms   p(95)=10.79ms
    transfer_post_req_duration............: avg=694.77ms min=83.66ms  med=631.8ms max=5.32s p(90)=918.47ms p(95)=1.09s

    HTTP
    http_req_duration.....................: avg=178.29ms min=905µs    med=6.42ms  max=5.32s p(90)=671.24ms p(95)=784.83ms
      { expected_response:true }..........: avg=178.29ms min=905µs    med=6.42ms  max=5.32s p(90)=671.24ms p(95)=784.83ms
    http_req_failed.......................: 0.00%  0 out of 400604
    http_reqs.............................: 400604 276.599907/s

    EXECUTION
    iteration_duration....................: avg=714.74ms min=147.23ms med=649.8ms max=5.35s p(90)=945.6ms  p(95)=1.13s
    iterations............................: 100000 69.045718/s
    vus...................................: 40     min=0           max=50
    vus_max...............................: 50     min=50          max=50

    NETWORK
    data_received.........................: 152 MB 105 kB/s
    data_sent.............................: 136 MB 94 kB/s




running (24m08.3s), 00/50 VUs, 100000 complete and 0 interrupted iterations
default ✓ [======================================] 50 VUs  23m49.7s/30m0s  100000/100000 shared iters
```

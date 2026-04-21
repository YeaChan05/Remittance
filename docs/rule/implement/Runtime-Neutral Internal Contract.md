### Runtime-Neutral Internal Contract

모듈 간 내부 호출은 보통 두 방향 중 하나로 흘러가기 쉽다

- 처음부터 같은 프로세스 호출만 가정하고 직접 bean을 참조한다
- 반대로 처음부터 네트워크 호출만 가정하고 consumer/service 코드까지 transport 세부사항에 물든다

현재 프로젝트는 이 둘을 절충하지 않고, **런타임에 중립적인 internal contract**를 먼저 고정하는 방식을 사용한다

핵심은 “같은 계약이 same-process 조립, 실제 HTTP 호출, 테스트 stub 모두를 관통해야 한다”는 점이다

#### 1. 계약 위치

internal contract는 provider 쪽 `api-internal` 모듈에 둔다

- contract 패키지: 외부가 import 가능한 계약 인터페이스와 요청/응답 DTO
- adapter 패키지: provider use case를 호출하는 controller/adapter 구현

예를 들면 아래처럼 Spring HTTP interface로 계약을 정의할 수 있다

```kotlin
@HttpExchange("/internal/{resources}")
fun interface {Domain}InternalApi {
    @PostExchange("/query")
    fun query(@RequestBody request: {Domain}QueryRequest): {Domain}QueryResponse
}
```

이렇게 하면 계약 자체가 특정 런타임 구현에 묶이지 않는다

#### 2. consumer 사용 방식

consumer는 `service`에서 provider 구현을 직접 보지 않는다

- `service`는 자신의 port만 호출한다
- `infrastructure`가 provider internal contract를 사용해 adapter를 만든다
- 결과는 필요하면 consumer local type으로 감싸서 service에 넘긴다

```kotlin
class {Domain}ClientAdapter(
    private val internalApi: ProducerInternalApi,
) : {Domain}Client {
    override fun query(id: Long): {Domain}Snapshot =
        internalApi.query(ProducerQueryRequest(id)).toSnapshot()
}
```

즉, transport 계약을 아는 곳은 consumer `infrastructure`까지로 제한한다

#### 3. 런타임 선택

이 패턴의 중요한 지점은 “어떻게 호출할지”를 service가 아니라 wiring이 결정한다는 점이다

- same-process 조립 시: provider `api-internal`의 adapter bean이 직접 주입될 수 있다
- remote 호출 시: `RestClient` + `HttpServiceProxyFactory` 같은 프록시로 같은 인터페이스를 구현한다
- 테스트 시: 같은 base-url 자리에 stub 서버를 물려 계약 수준에서 검증한다

예시:

```kotlin
@AutoConfiguration
class ConsumerInfrastructureBeanRegistrar :
    BeanRegistrarDsl({
        if (env.containsProperty("spring.http.serviceclient.sample-internal.base-url")) {
            registerBean<SampleInternalApi> {
                createInternalApiClient<SampleInternalApi>(
                    environment = bean(),
                    groupName = "sample-internal",
                )
            }
        }

        registerBean<SampleClient> {
            SampleClientAdapter(bean())
        }
    })
```

여기서 중요한 점은 `SampleClientAdapter`는 항상 같은 `SampleInternalApi` 계약만 본다는 것이다

#### 4. 테스트 방식

런타임 중립 계약은 테스트에서도 같은 계약을 유지할 때 가장 가치가 크다

권장 방식은 다음과 같다

- `DynamicPropertySource`로 internal base-url을 테스트 서버로 바꾼다
- `MockWebServer` 같은 lightweight HTTP stub를 띄운다
- contract path, request body, header, response shape를 실제처럼 맞춘다

이 방식은 consumer/service 코드를 mock 기반으로 느슨하게 검증하는 대신,
실제 transport 계약을 유지한 상태에서 consumer `infrastructure`와 상위 흐름을 검증할 수 있게 해준다

#### 5. 장점

- same-process와 remote 호출 사이를 비교적 부드럽게 오갈 수 있다
- service 계층이 transport 세부사항에 오염되지 않는다
- 테스트에서도 같은 contract를 재사용할 수 있다
- provider contract가 분리되어 있으므로 호출 경계가 문서화되기 쉽다
- 모듈러 모놀리스에서 시작해 이후 분리 가능성을 열어두는 데 유리하다

#### 6. 주의할 점

- consumer `service`가 provider request/response DTO를 직접 다루기 시작하면 경계가 무너진다
- contract 모듈에 provider 내부 구현 세부사항이 새어 나오면 안 된다
- same-process 최적화를 위해 service가 provider bean을 직접 참조하게 만들면 이 패턴의 장점이 사라진다
- remote stub 테스트는 contract를 지키는 데 유리하지만, provider 실제 구현 검증을 완전히 대체하지는 않는다

#### 7. 언제 유용한가

- 멀티 모듈 모놀리스
- 내부 호출이 많지만 아직 완전한 분산 시스템으로 나누고 싶지 않은 프로젝트
- 추후 same-process -> remote 분리 가능성을 열어두고 싶은 프로젝트
- consumer/service를 transport-neutral하게 유지하고 싶은 프로젝트

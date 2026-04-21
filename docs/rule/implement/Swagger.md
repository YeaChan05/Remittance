### Swagger / OpenAPI

API 명세 자동화 및 테스트 환경을 위해 Swagger(Springdoc OpenAPI)를 사용한다

#### 1. 설정 및 접근

- **모듈**: `aggregate` 모듈에서 전역 OpenAPI 설정을 관리한다
- **의존성**: `org.springdoc:springdoc-openapi-starter-webmvc-ui`를 사용한다
- **경로**: Swagger UI 진입점은 `/swagger-ui.html`, OpenAPI 문서는 `/v3/api-docs`를 사용한다
- **버전 헤더**: Swagger / OpenAPI 경로는 브라우저 접근을 위해 `X-API-Version` 헤더 없이 열려 있어야 한다

#### 2. 인증 설정 (SecurityScheme)

Swagger UI에서 인증된 요청을 테스트할 수 있도록 Bearer Token 인증 설정을 포함한다

- `OpenApiConfiguration`: `@SecurityScheme`을 통해 `bearerAuth` (JWT) 방식을 정의한다
- UI 상단의 'Authorize' 버튼을 통해 발급받은 JWT 토큰을 입력하면, 이후 모든 요청의 `Authorization` 헤더에 자동으로 포함된다

#### 3. 보안 정책과의 연동

`ApplicationOpenEndpointsAuthorizeHttpRequestsCustomizer`를 통해 Swagger 관련 경로는 항상 인증 없이 접근 가능(`permitAll`)하도록 설정되어 있다
또한 전역 API versioning이 켜져 있어도 Swagger / OpenAPI 경로는 예외 처리해 `X-API-Version` 헤더가 없어도 접근 가능해야 한다

#### 4. 문서화 권장 사항

- `@Operation`: API의 기능과 의도를 명확히 설명한다
- `@ApiResponse`: 성공 및 실패(비즈니스 예외 포함) 케이스에 대한 응답 구조를 명시한다
- `@Parameter`: 경로 변수나 쿼리 파라미터에 대한 설명을 추가한다


#### 5. 적용 방식

실제 문서 작성은 `Controller`에 직접 하는것이 아니라 `interface`로 분리하여 명시한다

```kotlin
@RestController  
@RequestMapping("/{domain}s")  
class {Domain}Controller() : {Domain}Api {
	@PostMapping  
	override fun create() : ResponseEntity<{Domain}Response> {
		...
	}
}

@Tag(name = "{Domain}", description = "{Domain} API")  
interface {Domain}Api {  
    @Operation(summary = "Create {domain}", description = "Registers a {domain}.")  
    @ApiResponses(  
        ApiResponse(responseCode = "200", description = "Success", content = [Content()]),  
    )
    fun create(): ResponseEntity<{Domain}Response>
    
    ...
}
```

endpoint와 문서를 분리한다

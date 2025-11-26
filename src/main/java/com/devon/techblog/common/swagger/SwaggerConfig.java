package com.devon.techblog.common.swagger;

import com.devon.techblog.common.exception.ErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("Techblog API")
                .description("카카오테크부트캠프 커뮤니티 API 문서")
                .version("1.0.0");
    }

    @Bean
    public OperationCustomizer customOperationCustomizer() {
        return (operation, handlerMethod) -> {
            CustomExceptionDescription annotation = handlerMethod.getMethodAnnotation(CustomExceptionDescription.class);

            if (annotation != null) {
                SwaggerResponseDescription responseDescription = annotation.value();
                ApiResponses apiResponses = operation.getResponses();

                for (ErrorCode errorCode : responseDescription.getErrorCodeList()) {
                    String statusCode = String.valueOf(errorCode.getHttpStatus().value());
                    String description = errorCode.getMessage();

                    java.util.Map<String, Object> exampleMap = new java.util.HashMap<>();
                    exampleMap.put("success", false);
                    exampleMap.put("data", null);
                    exampleMap.put("message", errorCode.getMessage());

                    Schema<?> errorSchema = new Schema<>()
                            .type("object")
                            .addProperty("success", new Schema<>().type("boolean").example(false))
                            .addProperty("data", new Schema<>().type("object").nullable(true).example(null))
                            .addProperty("message", new Schema<>().type("string").example(errorCode.getMessage()));

                    ApiResponse apiResponse = new ApiResponse()
                            .description(description)
                            .content(new Content()
                                    .addMediaType("application/json",
                                            new MediaType()
                                                    .schema(errorSchema)
                                                    .example(exampleMap)
                                            )
                            );

                    apiResponses.addApiResponse(statusCode, apiResponse);
                }
            }

            return operation;
        };
    }
}

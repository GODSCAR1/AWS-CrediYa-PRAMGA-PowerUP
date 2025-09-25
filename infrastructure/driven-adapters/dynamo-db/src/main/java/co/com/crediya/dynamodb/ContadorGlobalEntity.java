package co.com.crediya.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.math.BigDecimal;

/* Enhanced DynamoDB annotations are incompatible with Lombok #1932
         https://github.com/aws/aws-sdk-java-v2/issues/1932*/
@DynamoDbBean
public class ContadorGlobalEntity {

    private String id;
    private Long cantidadPrestamosAprobados;
    private BigDecimal cantidadTotalPrestada;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")  // Cambié de "name" a "id" para mayor claridad
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbAttribute("cantidadPrestamosAprobados")
    public Long getCantidadPrestamosAprobados() {
        return cantidadPrestamosAprobados;
    }

    public void setCantidadPrestamosAprobados(Long cantidadPrestamosAprobados) {
        this.cantidadPrestamosAprobados = cantidadPrestamosAprobados;
    }

    @DynamoDbAttribute("cantidadTotalPrestada")
    public BigDecimal getCantidadTotalPrestada() {
        return cantidadTotalPrestada;
    }

    public void setCantidadTotalPrestada(BigDecimal cantidadTotalPrestada) {
        this.cantidadTotalPrestada = cantidadTotalPrestada;
    }

}

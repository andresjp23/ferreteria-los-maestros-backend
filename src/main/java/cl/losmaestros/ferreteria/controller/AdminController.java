package cl.losmaestros.ferreteria.controller;

import cl.losmaestros.ferreteria.dto.ResumenResponse;
import cl.losmaestros.ferreteria.service.ResumenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin · Dashboard", description = "Indicadores del panel de administración")
public class AdminController {

    private final ResumenService resumenService;

    public AdminController(ResumenService resumenService) {
        this.resumenService = resumenService;
    }

    @GetMapping("/resumen")
    @Operation(summary = "Productos, usuarios, pedidos y ventas del mes, con su variación vs el mes anterior")
    public ResumenResponse resumen() {
        return resumenService.resumen();
    }
}

package com.concesionario.controller;



import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.concesionario.model.Cita;
import com.concesionario.model.Usuario;
import com.concesionario.model.Vehiculo;
import com.concesionario.repository.UsuarioRepository;
import com.concesionario.service.CitaService;
import com.concesionario.service.UsuarioService;
import com.concesionario.service.VehiculoService;

@Controller
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private CitaService citaService;

    @Autowired
    private VehiculoService vehiculoService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;



    @GetMapping("/perfil")
    public String verPerfil(Model model, Principal principal) {
        String email = principal.getName();

        // Ya lanza excepción internamente
        Usuario usuario = usuarioService.findByCorreoUser(email);
        List<Cita> citas = citaService.obtenerCitasPorUsuarioId(usuario.getId());

        // List<Cita> citas = citaService.findByUsuarioId(usuario.getId());
        model.addAttribute("nombreUsuario", usuario.getNombreUser());
        model.addAttribute("citas", citas);
        return "usuario/perfil";
    }

    @GetMapping("/agendamiento")
    public String Inicioff() {
        return "agendamiento";
    }


    @GetMapping("/Inicio")
    public String Inicio(Model model) {
        List<Vehiculo> vehiculos = vehiculoService.obtenerTodos();

        // Agrupar por categoría con manejo de nulos
        Map<String, List<Vehiculo>> vehiculosPorCategoria = vehiculos.stream()
                .filter(v -> v.getCategoria() != null) // Filtra vehículos sin categoría
                .collect(Collectors.groupingBy(Vehiculo::getCategoria));

        // Obtener destacados con verificación
        List<Vehiculo> vehiculosDestacados = Optional.ofNullable(vehiculoService.obtenerDestacados())
                .orElse(Collections.emptyList());

        // Añadir atributos al modelo
        model.addAttribute("categorias", vehiculosPorCategoria.keySet());
        model.addAttribute("vehiculosPorCategoria", vehiculosPorCategoria);
        model.addAttribute("destacados", vehiculosDestacados);

        return "index";
    }


    @GetMapping("/cita")
    public String mostrarFormularioCita(
            @RequestParam(required = false) String vehiculoId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String color,
            Principal principal,
            Model model) {

        // Obtener usuario autenticado
        Usuario usuario = usuarioRepository.findByCorreoUser(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Crear nueva cita con datos del usuario
        Cita cita = new Cita();
        cita.setNombres(usuario.getNombreUser());
        cita.setApellidos(usuario.getApellidoUser());
        cita.setCedula(usuario.getIdentificacionUser());
        cita.setCorreoElectronico(usuario.getCorreoUser());
        // cita.setTelefono(usuario.getTelefonoUser()); // Si tienes teléfono en usuario

        List<Vehiculo> vehiculos = vehiculoService.obtenerTodos();

        // Configuración basada en parámetros
        if (vehiculoId != null) {
            Vehiculo vehiculo = vehiculoService.obtenerPorId(vehiculoId);
            if (vehiculo != null) {
                // cita.setTipo("Interés en Vehículo"); //esto hay que quitarlo:
                cita.setVehiculoId(vehiculo.getId());
                cita.setNombreVehiculo(vehiculo.getMarca() + " " + vehiculo.getModelo());
                cita.setColorVehiculo(color);
                model.addAttribute("coloresVehiculo",
                        vehiculo.getColores() != null ? vehiculo.getColores() : Collections.emptyList());
            }
        } else if (tipo != null) {
            cita.setTipo(tipo);
        } else {
            cita.setTipo("Otros");
        }

        model.addAttribute("cita", cita);
        model.addAttribute("vehiculos", vehiculos);
        return "cita";
    }



    @PostMapping("/cita/guardar")

    public String guardarCita(@ModelAttribute Cita cita, 
                              Principal principal, 
                              RedirectAttributes redirectAttributes) {
    try {
        // 1. Validar usuario
        Usuario usuario = usuarioRepository.findByCorreoUser(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Validar datos inmutables (nombres, cédula, etc.)
        if (!validarDatosInmutables(cita, usuario)) {
            redirectAttributes.addFlashAttribute("error", "No puedes modificar tus datos personales");
            return "redirect:/usuario/cita";
        }

        // citaService.crearCitaConEmbedding(cita, usuario, vehiculo); 
        // 3. Lógica diferenciada por tipo de cita
        Vehiculo vehiculo = null;
        if (cita.getVehiculoId() != null) {
            vehiculo = vehiculoService.obtenerPorId(cita.getVehiculoId());
            if (vehiculo == null) {
                redirectAttributes.addFlashAttribute("error", "Vehículo no encontrado");
                return "redirect:/usuario/cita";
            }
        }
        if("Otros".equals(cita.getTipo())){
            citaService.guardarCitaSimple(cita, usuario, vehiculo);
        } else {
            citaService.crearCitaConEmbedding(cita, usuario, vehiculo);
        }

        
        return "redirect:/usuario/perfil?success=Cita+agendada+exitosamente";
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "Error al agendar la cita: " + e.getMessage());
        return "redirect:/usuario/cita";
    }
    }
    
    
    private boolean validarDatosInmutables(Cita cita, Usuario usuario) {
        return usuario.getNombreUser().equals(cita.getNombres()) &&
               usuario.getApellidoUser().equals(cita.getApellidos()) &&
               usuario.getIdentificacionUser().equals(cita.getCedula()) &&
               usuario.getCorreoUser().equals(cita.getCorreoElectronico());
    }

    @GetMapping("/mis-citas")
    public String verMisCitas(Model model, Principal principal) {

        Usuario usuario = usuarioRepository.findByCorreoUser(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));


        List<Cita> citas = citaService.obtenerCitasPorUsuarioId(usuario.getId());
        model.addAttribute("citas", citas);
        model.addAttribute("nombreUsuario", usuario.getNombreUser() + " " + usuario.getApellidoUser());
        model.addAttribute("citas", citas);
        return "Usuario/MisCitas";
    }

}

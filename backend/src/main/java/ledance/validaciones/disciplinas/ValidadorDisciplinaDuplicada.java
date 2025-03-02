//package ledance.validaciones.disciplinas;
//
//import ledance.dto.disciplina.request.DisciplinaRegistroRequest;
//import ledance.repositorios.DisciplinaRepositorio;
//import ledance.validaciones.Validador;
//import org.springframework.stereotype.Component;
//
//@Component
//public class ValidadorDisciplinaDuplicada implements Validador<DisciplinaRegistroRequest> {
//
//    private final DisciplinaRepositorio disciplinaRepositorio;
//
//    public ValidadorDisciplinaDuplicada(DisciplinaRepositorio disciplinaRepositorio) {
//        this.disciplinaRepositorio = disciplinaRepositorio;
//    }
//
//    @Override
//    public void validar(DisciplinaRegistroRequest datos) {
//        if (disciplinaRepositorio.existsByNombreAndHorarioInicio(datos.nombre(), datos.horarioInicio())) {
//            throw new RuntimeException("La disciplina ya esta registrada con el mismo nombre y horario: "
//                    + datos.nombre());
//        }
//    }
//}

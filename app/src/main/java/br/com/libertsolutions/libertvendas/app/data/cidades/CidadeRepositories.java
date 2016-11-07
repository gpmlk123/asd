package br.com.libertsolutions.libertvendas.app.data.cidades;

import android.content.Context;
import android.support.annotation.NonNull;
import br.com.libertsolutions.libertvendas.app.data.repository.Mapper;
import br.com.libertsolutions.libertvendas.app.data.repository.Repository;
import br.com.libertsolutions.libertvendas.app.data.util.ServiceFactory;
import br.com.libertsolutions.libertvendas.app.domain.entity.CidadeEntity;
import br.com.libertsolutions.libertvendas.app.domain.pojo.Cidade;

/**
 * @author Filipe Bezerra
 */
public class CidadeRepositories {
    private CidadeRepositories() {/* No instances */}

    private static CidadeService sService = null;

    private static Repository<Cidade> sRepository = null;

    private static Mapper<Cidade, CidadeEntity> sMapper = null;

    public static synchronized CidadeService getService(@NonNull Context pContext) {
        if (sService == null) {
            sService = ServiceFactory.createService(pContext, CidadeService.class);
        }
        return sService;
    }

    public static synchronized Repository<Cidade> getRepository(@NonNull Context pContext) {
        if (sRepository == null) {
            sRepository = new CidadeRepository(pContext, getEntityMapper());
        }
        return sRepository;
    }

    public static Mapper<Cidade, CidadeEntity> getEntityMapper() {
        if (sMapper == null) {
            sMapper = new CidadeMapper(EstadoRepositories.getEstadoMapper());
        }
        return sMapper;
    }


}
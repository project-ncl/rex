package org.jboss.pnc.scheduler.facade.mapper;

/**
 * Mapper that converts database entity to DTO and vice versa
 * @param <DTO> DTO entity type
 * @param <DB> Database entity type
 */
public interface EntityMapper<DTO, DB> {
    /**
     * Converts database entity to DTO entity.
     * @param dbEntity database entity
     * @return Converted DTO entity
     */
    DTO toDTO(DB dbEntity);


    /**
     * Converts DTO entity to database entity.
     * @param dtoEntity DTO entity.
     * @return Converted database entity.
     */
    DB toDB(DTO dtoEntity);
}

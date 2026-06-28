package com.camplog.connection.model;

/**
 * Enum que representa os tipos de perfil existentes no CampLog.
 *
 * DECISÃO ARQUITETURAL: Este enum vive no módulo de conexões (e não no módulo profile)
 * porque representa um conceito do domínio de relacionamentos. Ele é a chave do
 * polimorfismo — permite que a tabela de conexões referencie qualquer tipo de perfil
 * sem criar foreign keys diretas para as tabelas de perfil, mantendo o isolamento
 * entre os módulos.
 *
 * Se no futuro novos tipos de perfil surgirem (ex: ORGANIZATION), basta adicionar
 * o valor aqui sem alterar a estrutura da tabela.
 */
public enum ProfileType {
    CREATOR,
    SUPPORTER
}

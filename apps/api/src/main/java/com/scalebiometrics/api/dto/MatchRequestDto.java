package com.scalebiometrics.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequestDto {
    // L'identifiant de la personne (Identity ID)
    private String probeRid;
    
    // Nombre de résultats souhaités (pour le matching)
    private int topK;
    
    // Seuil de score pour considérer un match
    private float threshold;
    
    // Métadonnées optionnelles (ex: nom, prénom pour l'enrôlement)
    private String metadata;
    private byte[] probeTemplate;
    private float[] probeEmbedding;
}

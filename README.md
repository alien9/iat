IAT

#Investigação de Acidentes de Trânsito

Esta aplicação cria registros de incidentes de trânsito. 



#Configurações
* Dependências
    * Spatialite
        https://www.gaia-gis.it/fossil/libspatialite/wiki?name=splite-android
    
A aplicação renderiza mapas de acordo com a localização fornecida na inicialização ou a partir da leitura de dados de GPS.
Estão configurados dois layers de renderização de mapas: o layer básico com contornos de quadras e o de rótulos de logradouros.  
O layer de desenho de quadras é definido pela tabela _quadras_ com a geometria configurada com o sistema de coordenadas WGS84 (EPSG 4326), assim como o de logradouros com a tabela _logradouros_. O campo _nome_ na tabela logradouros determina qual será o campo usado para rotular os locais.

O banco de dados pode ser gerado a partir dos arquivos ESRI Shapefile correspondentes ao mapa de quadras e logradouros.



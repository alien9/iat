<?php
switch($_SERVER['SERVER_NAME']){
#case 'localhost':
case '192.168.0.120':
	//Local
	$dsn = "mysql:localhost;port=3306;dbname=galaxynotevivo";
	$usuario = "root";
	$senha = "ludovico";	
	$opcoes="";
	$convert='convert';
	
break;
default:
	//Produção
	$dsn = "mysql:host=mysql01.galaxynotevivo.hospedagemdesites.ws;port=3306;dbname=galaxynotevivo";
	$usuario = "galaxynotevivo";
	$senha = "PwT41iBtif";
	$convert='convert';
}

// Conexao via PDO
$opcoes = array(
    PDO::ATTR_PERSISTENT => false,
    PDO::ATTR_CASE => PDO::CASE_LOWER
);

try {
	$pdo = new PDO($dsn, $usuario, $senha, $opcoes);
	$pdo->exec('SET NAMES utf8'); 
	$pdo->exec("SET GLOBAL general_log = 'ON'"); 
}catch (PDOException $e){
  echo 'Erro: '.$e->getMessage();
}
?>

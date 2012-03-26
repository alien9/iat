<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Relatório Galaxy Note Vivo</title>
<? include_once("config.php"); ?>
</head>

<body>

<table class="table_listas" border="1" cellpadding="10">
  <tr style="background-color:#666; color:white">
  	<th width="40" align="center">Id</th>
    <th>Nome</th>
    <th>CPF</th>
    <th>Email</th>
    <th>Telefone</th>
    <th>Endereço</th>
    <th>Cidade</th>
    <th>IMEI</th>
    <th>Deseja receber</th>
    <th width="140">Inserido</th>
    <th width="140">Alterado</th> 
  </tr>
  <?
  //''''''''''''''''''''''''''''''''''''''''''''''''''''''MONTAGEM DA LISTAGEM
	$sql="SELECT * FROM participantes ORDER BY id";
  $stmt = $pdo->query($sql);
  $i = 0;
  foreach ($stmt as $r){
    $i++;
    if ($i%2==0){ $cor_tr="background-color:#CCC"; } else { $cor_tr="background-color:#F1F1F1"; }
		?>
    <tr style="<?=$cor_tr?>">
      <td align="center"><?=$r["id"]?></td>
      <td><?=$r["nome"]?></td>
      <td><?=$r["cpf"]?></td>
      <td><?=$r["email"]?></td>
      <td><?=$r["tel"]?></td>
      <td><?=$r["endereco"]?></td>
      <td><?=$r["cidade"]?></td>
      <td><?=$r["imei"]?></td> 
      <td><?=$r["desejareceber"]?></td> 
      <td><?=date('d/m/Y H:i:s', strtotime($r["inserido"]));?></td>       
      <td><?=date('d/m/Y H:i:s', strtotime($r["alterado"]));?></td>       
    </tr>								
    <?	
  }
  //'''''''''''''''''''''''''''''''''''''''''FIM DA MONTAGEM DA LISTAGEM
  ?> 									
</table>

</body>
</html>
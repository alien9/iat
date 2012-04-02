<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>Galaxy Note Vivo</title>
<meta name="Keywords" content="" /> 
<meta name="Description" content="" /> 
<meta name="robots" content="index, follow">
<link rel="icon" href="/favicon.ico" type="image/x-icon">
<link rel="shortcut icon" href="/favicon.ico" type="image/x-icon">
<link href='http://fonts.googleapis.com/css?family=Nunito' rel='stylesheet' type='text/css'>
<script src="jquery.js"></script>

<script type="text/javascript">
function popUp(p){
	$('#pop_regulamento').fadeOut();
	$('#pop_lojas').fadeOut();
	$('#pop_faleconosco').fadeOut('slow', function() {
    	$('#'+p).fadeIn();
  	});
}
</script>

<style type="text/css">

body, html{ height:100% }
body{ margin:0; background-image:url(img/bg.gif); font-family: 'Nunito', sans-serif; font-size:14px }
a{ border:0 }
h4{ color:#F7941D; margin:0 }
#container{ position:relative; margin-left:auto; margin-right:auto; top:0; width:1190px; min-height:100% }
#header{ position:absolute; top:0; height:168px; width:100% }
#estrelinhas{ position:absolute; top:157px; left:40px; z-index:10 }
#balao{ position:absolute; top:60px; right:40px; z-index:10 }
#home{ position:absolute; top:267px; left:40px }
#banner{ position:absolute; top:727px; left:90px }
#capinhas{ position:absolute; top:950px; left:70px; padding-bottom:80px }
#oucompre{ position:absolute; top:1016px; left:810px }

#pop_faleconosco{ position:absolute; left:220px; top:100px; z-index:20; display:none }
#pop_regulamento{ position:absolute; left:220px; top:700px; z-index:30; display:none }
#pop_lojas{ position:absolute; left:220px; top:600px; z-index:40; display:none }

#pop_regulamento div{ 
	position:absolute; 
	left:50px; top:120px; 
	z-index:31; 
	width:636px; height:470px; 
	overflow:scroll; 
	font-size:10px;
	border:1px solid #83BBE3;
	padding:20px
}

#pop_lojas table{ 
	position:absolute; 
	left:48px; top:100px; 
	z-index:41; 
	width:736px; height:470px; 
	font-size:12px;
	padding:20px;
	color:#0014A0
}

</style>

</head>
<body>
<div id="container">

    <div id="header">
    	<center><img src="img/logo.gif" /></center>
        <a href="javascript:void(0)" style="color:#FFF; position:absolute; top:20px; right:20px" onclick="popUp('pop_faleconosco')">Fale conosco</a>
    	<img src="img/estrelinhas.png" id="estrelinhas" />
    	<img src="img/balao.png" id="balao" />
    </div>
    
    <div id="home">
    	<center><img src="img/home.png" border="0" usemap="#Map" />
          <map name="Map" id="Map">
            <area shape="rect" coords="-5,209,192,373" href="http://apps.facebook.com/galaxynotebrasil/" target="_blank" />
          </map>
    	</center>
    </div>

	<div id="banner">
    	<center>
        <a href="javascript:void(0)" onclick="popUp('pop_lojas')"><img src="img/banner.png" /></a>
      </center>
    </div>
    
    <div id="capinhas">
    	<center><img src="img/capinhas.jpg" /></center>
    </div>
    
    <div id="oucompre">
    	<center><img src="img/oucompre.png" /></center>
        <a href="javascript:void(0)" style="color:#0014A0; position:absolute; top:240px" onclick="popUp('pop_regulamento')">Regulamento</a>
    </div>
    
    <div id="pop_faleconosco">
    	<img src="img/pop_faleconosco.png" usemap="#Map_faleconosco" />
        <map name="Map_faleconosco" id="Map_faleconosco">
          <area shape="rect" coords="706,27,748,58" href="javascript:void(0)" onclick="$('#pop_faleconosco').fadeOut()" />
          <area shape="rect" coords="96,217,503,302" href="mailto:contato@galaxynotevivo.com.br" />
        </map>
  	</div>
    
    <div id="pop_regulamento">
    	<img src="img/pop_regulamento.png" usemap="#Map_regulamento" />
        <map name="Map_regulamento" id="Map_regulamento">
          <area shape="rect" coords="706,27,748,58" href="javascript:void(0)" onclick="$('#pop_regulamento').fadeOut()" />
        </map>
      	<div><? include_once('regulamento.html'); ?></div>
	</div>
    
    <div id="pop_lojas">
    	<? include_once('lojas.html'); ?>
    	<img src="img/pop_lojas.png" usemap="#Map_lojas" />
        <map name="Map_lojas" id="Map_lojas">
          <area shape="rect" coords="706,27,748,58" href="javascript:void(0)" onclick="$('#pop_lojas').fadeOut()" />
        </map>
    </div>
    
</div>
</body>
</html>
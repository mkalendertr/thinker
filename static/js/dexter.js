function embedding(){
	var textarea = document.getElementById('searchbox');
	text = textarea.value
	//alert("annotate: "+text.substring(0,100)+"...")
	//
//	$.getJSON("./api/rest/annotate?text="+text,
//	function(data) {
//			//alert(val)
//			annotate = data.annotatedDocument.content;
//			document.getElementById("result").innerHTML = annotate;
//			document.getElementById('input').style.display="none";
//			document.getElementById('output').style.display="block";
//		
//		}
//	);
	$.ajax({
	    type: "GET",
	    url: "./api/rest/embedding?text="+encodeURIComponent(text),
	    contentType: "application/json; charset=utf-8",
	    dataType: "json",    
	    success: function(data) {
	    	document.getElementById("info").innerHTML = "<p>"+data.description+"</p>"
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	        alert("Error: " + textStatus + " errorThrown: " + errorThrown);
	    }
	});  

}

function annotateText(){
	var textarea = document.getElementById('inputtext');
	text = textarea.value
	var spotter = document.getElementById('myspotter');
	spotter = spotter.value
	//alert("annotate: "+text.substring(0,100)+"...")
	//
//	$.getJSON("./api/rest/annotate?text="+text,
//	function(data) {
//			//alert(val)
//			annotate = data.annotatedDocument.content;
//			document.getElementById("result").innerHTML = annotate;
//			document.getElementById('input').style.display="none";
//			document.getElementById('output').style.display="block";
//		
//		}
//	);
	$.ajax({
	    type: "POST",
	    url: "./api/rest/annotate?myspotter="+spotter+"&text="+encodeURIComponent(text),
	    contentType: "application/json; charset=utf-8",
	    dataType: "json",    
	    success: function(data) {
	    	annotate = data.annotatedDocument.content;
			document.getElementById("result").innerHTML = annotate;
			document.getElementById('input').style.display="none";
			document.getElementById('output').style.display="block";
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	        alert("Error: " + textStatus + " errorThrown: " + errorThrown);
	    }
	});  

}



function manage(id, type, prediction){

	//
	$.getJSON("./api/rest/get-desc?id="+id,
	function(data) {
			//alert(val)
			title = data.title
			document.getElementById("info").innerHTML = "<p><a href=\""+data.uri+"\">"+data.uri+"</a></p>"
			document.getElementById("info").innerHTML += "<strong>"+title+"</strong><div id='desc'>"
//			if (data.image != '' ){
//				document.getElementById("info").innerHTML += "<img src='"+data.image+"' width='150' style='{float:left}'/>"
//			}
			document.getElementById("info").innerHTML += "<p>"+data.description+"</p>"
			document.getElementById("info").innerHTML += "<strong>Type:</strong> "+type+", <strong>Prediction:</strong>" +prediction+" "
			
	}
	);

}

function reset(){
	document.getElementById('output').style.display="none";
	document.getElementById('input').style.display="block";
}
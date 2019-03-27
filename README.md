Utilities for Offer-Ready XSLT software.

Contributions are welcome. Please open an issue describing what you wish to achieve. We will be able to help you with advice, before you invest the time of development. When you've developed your patch, please submit a pull request using github.

XSLT Transformation
-------------------
All XSLT transformation uses XSLT 2.0, and is applied using the Java library Saxon.

By default the Saxon-HE product is used, which is open-source and requires no license fees.

If Saxon-PE is available (see "deployment.md" how to install and use Saxon-PE), then the following functions are available to XSLT:

* `<xsl:value-of select="uuid:randomUUID()" xmlns:uuid="java:java.util.UUID"/>`

* `<xsl:value-of select="digest:sha256Hex('foo')" xmlns:digest="java:org.apache.commons.codec.digest.DigestUtils"/>`

Transformers
------------
The root element can have the child element `<xslt-file name="my-transformation.xslt"/>`. If present, this references an XSLT file in the `data-source-xslt` directory of the application. If not present, no XSLT transformation is done.

The root elements can have elements like `<placeholder-value placeholder-name="x" value="y"/>` which will be passed to the XSLT processing as `<xsl:param>`. Note that these are distinct from the other variables described in this document, `${x}` etc. will not work.

In addition, the following elements may be present:

* `<convert-output-xsl-fo-to-pdf/>`. If present, this means that the XML is assumed to be XSL-FO and a transformation from that into PDF is done. The Apache FOP library is used to perform this transformation. The content type "application/pdf" is set.

  In the `fonts` directory there may be a file `apache-fop-config.xml` and any number of font files. See the `example-customer` for an example of that file's syntax.
  
  The XSL-FO file may reference images with XSL-FO commands such as `<fo:external-graphic src="spheres.png"/>`. In that case, the images should be in the `static` directory of the application. Of course you are free to use absolute URLs to any resource available on the internet as well.
  
  Previously this tag was called `<convert-output-xml-fo-to-pdf/>` and this tag is still supported.
  
* `<convert-output-xml-to-json/>`. The result of the transformation is assumed to be XML and it is converted to JSON. The content type "application/json" is set.
  
* `<convert-output-xml-to-excel>`. If present, the output of the XSLT is assumed to be HTML. See below for more information. The content type "application/ms-excel" is used.

* If none of the above tags are present, then the XML produced from the XSLT is returned to the client, default content type "text/plain".

The tag `<content-type type="text/html"/>` may be present. If present, you may set the content-type of the document. The tags above automatically set the content-type to an appropriate value. This tag can override the default value produced by the tags above, or is useful if no such tags are used e.g. if the XSLT outputs HTML and no further transformation to PDF etc. is necessary.

HTML to Excel conversion
------------------------
If the `<convert-output-xml-to-excel>` is used, as specified above, then HTML is converted to Excel binary format. The format is chosen to be as similar to XHTML as possible. The syntax is as follows: 

* HTML should contain `<table>` elements. 
* These should contain `<tr>` elements and within them `<td>` (or `<th>`) elements. 
* Excel files differentiate between "text cells" and "number cells". The contents of the `<td>` are inspected to see if they look like a number, in which case an Excel "number cell" is produced, otherwise an Excel "text cell" is produced.
* The attribute `<convert-output-xml-to-excel input-decimal-separator="xxx">` affects how numbers in the input HTML document are parsed.
  * "dot" (default). Decimal separator is ".", thousand separator is ",".
  * "comma". Decimal separator is ",", thousand separator is ".".
  * "magic". Numbers may use either dot or comma as thousand or decimal separator. Heuristics are used to determine which system is in use. (This is useful in very broken input documents that use dot for some numbers and comma for others, within the same document.) The numbers must either have zero decimal (e.g. "1,024") or two decimal places (e.g. "12,34"). Any other number of decimal places in the input data will lead to wrong results. 
* The number of decimal places in the `<td>` data are taken over the to Excel cell formatting. That is to say, `<td>12.20</td>` will produce an Excel number cell containing the value 12.2 with the Excel number format showing two decimal places, so will appear as 12.20 in the Excel file.
* To force the cell to be an Excel text cell, even if the above algorithm would normally classify it as an Excel number cell, make the table cell with `<td excel-type="text">`.
* The colspan attribute, e.g. `<td colspan="2">`, is respected. 
* The following style elements of `<td>` are respected:
  * `style="text-align: center"` (Right align etc. is not supported)
  * `style="font-weight: bold"`
  * `style="border-top:"` (Bottom borders etc. are not supported)
  * `style="color: green"`, `style="color: red"`, `style="color: orange"` (Other colors are not supported.)
* `<thead>`, `<tfoot>` and `<tbody>` are respected. (Elements in `<tfoot>` sections will appear at the bottom of the Excel file, no matter what order the tags come in in the HTML.) 
* Column widths are determined by the lengths of text within each column. 
* Any `<table>` which appears inside a `<td>` is ignored (i.e. tables may be nested in the HTML, only the outermost table is present in the resulting Excel file.) 
* Any other elements such as `<html>` or `<span>` are ignored; this allows most HTML to be used as-is. 
* Table rows which contain only table cells which contain no text are ignored. (Often such rows contain sub-tables, which themselves are ignored. Having empty rows doesn't look nice.) 


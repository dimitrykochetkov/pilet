package dmk.app.harjumaatransportcardbalance.network

interface ResponseCallback {

    fun onSuccess(value: String)

    fun onError(values: Array<String>)
}
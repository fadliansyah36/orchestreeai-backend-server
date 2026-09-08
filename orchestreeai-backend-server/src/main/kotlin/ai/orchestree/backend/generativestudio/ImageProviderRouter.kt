package ai.orchestree.backend.generativestudio

class ImageProviderRouter {
    fun routeProvider(modelPreference: String?): String {
        return "gpt-image-2"
    }
}

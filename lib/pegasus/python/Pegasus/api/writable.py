import json
from pathlib import Path

from Pegasus import yaml

__all__ = []


class _CustomEncoder(json.JSONEncoder):
    def default(self, obj):
        # TODO: handle instance of Date and Path
        """
        if isinstance(obj, Date):
			return "whatever spec we come up with for Date such as ISO8601"
        elif isinstance(obj, Path):
            return obj.resolve
        """

        if hasattr(obj, "__json__"):
            if callable(obj.__json__):
                return obj.__json__()
            else:
                raise TypeError("__json__ is not callable for {}".format(obj))

        return json.JSONEncoder.default(self, obj)


def _filter_out_nones(_dict):
    """Helper function to remove keys where their values are set to None to avoid cluttering yaml/json files
    
    :param _dict: object represented as a dict
    :type _dict: dict
    :raises ValueError: _dict must be of type dict
    :return: new dictionary with 'None' values removed 
    :rtype: dict
    """
    if not isinstance(_dict, dict):
        raise TypeError(
            "invalid _dict: {}; _dict must be of type {}".format(_dict, type(dict))
        )

    return {key: value for key, value in _dict.items() if value is not None}


class Writable:
    """Derived class can be serialized to a json or yaml file"""

    FORMATS = {"yml", "yaml", "json"}

    def _write(self, file, _format):
        """Internal function to dump to file in either yaml or json formats
        
        :param file: file object to write to 
        :type file: file
        :param _format: file format that can be "yml", "yaml", or "json
        :type _ext: str
        :raises ValueError: _format must be one of "yml", "yaml" or "json"
        """
        if _format.lower() not in Writable.FORMATS:
            raise ValueError(
                "invalid _ext: {_format}, extension must be one of {formats}".format(
                    _format=_format, formats=Writable.FORMATS
                )
            )

        if _format == "yml" or _format == "yaml":
            # TODO: figure out how to get yaml.dump to recurse down into nested objects
            # yaml.dump(_CustomEncoder().default(self), file, sort_keys=False)
            yaml.dump(
                json.loads(json.dumps(self, cls=_CustomEncoder)),
                file,
                allow_unicode=True,
            )
        else:
            json.dump(self, file, cls=_CustomEncoder, indent=4, ensure_ascii=False)

    def write(self, file, _format="yml"):
        """Serialize this class as either yaml or json and write to the given
        file.
        
        :param file: path or file object (opened in "w" mode) to write to, defaults to None
        :type file: str or file, optional
        :param _format: can be either "yml", "yaml" or "json", defaults to "yml"
        :type _format: str, optional
        :raises ValueError: _format must be one of "yml", "yaml" or "json"
        :raises TypeError: file must be a str or file object
        """
        if _format.lower() not in Writable.FORMATS:
            raise ValueError(
                "invalid file format: {_format}, format should be one of 'yml', 'yaml', or 'json'"
            )

        if isinstance(file, str):
            path = Path(file)
            ext = path.suffix[1:].lower()

            with open(file, "w") as f:
                if ext in Writable.FORMATS:
                    self._write(f, ext)
                else:
                    self._write(f, _format)

        elif hasattr(file, "read"):
            try:
                ext = Path(str(file.name)).suffix[1:]
            except AttributeError:
                # writing to a stream such as StringIO or TemporaryFile with
                # no attr "name"
                self._write(file, _format)
            else:
                if ext in Writable.FORMATS:
                    self._write(file, ext)
                else:
                    self._write(file, _format)

        else:
            raise TypeError(
                "{file} must be of type str or file object".format(file=file)
            )
